package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.plan.PlanId;
import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.model.procesamiento.ReclamoId;
import com.ensambla.backend.domain.model.procesamiento.ResultadoIntento;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.registro.crearRegistro.CrearRegistro;
import com.ensambla.backend.domain.useCase.registro.crearRegistro.CrearRegistroRequest;
import com.ensambla.backend.support.LecturasInmediatasDePrueba;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.ProyectosDePrueba;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ControlPlanPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProyectosDePrueba proyectosDePrueba;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private RegistroDatosService registroDatosService;

    @Autowired
    private RegistroRepository registroRepository;

    @Autowired
    private CrearRegistro crearRegistro;

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private ControlPlanService controlPlanService;

    @Autowired
    private LecturasInmediatasDePrueba consumoIaInmediatoService;

    @Autowired
    private UsoPlanRepository usoPlanRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ProcesamientoRepository procesamientoRepository;

    private UUID usuarioId;
    private UUID negocioId;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        negocioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Cuotas')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Cuotas', ?)",
                negocioId, usuarioId);
        suscripcionService.asignarGratuito(NegocioId.de(negocioId), AHORA.minusDays(30));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId);
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioId);
    }

    @Test
    @DisplayName("Dos altas simultáneas de sistemas gratuitos permiten exactamente una")
    void crearProyectoShouldSerializeConcurrentCreates() throws Exception {
        List<Boolean> result = concurrently(
                () -> crearProyecto("Primero"), () -> crearProyecto("Segundo"));

        assertThat(result).containsExactlyInAnyOrder(true, false);
        assertThat(usoPlanRepository.contarProyectos(NegocioId.de(negocioId))).isEqualTo(1);
    }

    @Test
    @DisplayName("La cuota de registros es por negocio, con altas simultáneas en bloques distintos")
    void crearRegistroShouldCountAcrossBlocksAtomically() throws Exception {
        UUID bloqueA = givenBloque("Productos");
        UUID bloqueB = givenBloqueInSameProyecto(bloqueA, "Ventas");
        givenRegistros(bloqueA, 999);

        List<Boolean> result = concurrently(
                () -> crearRegistro(bloqueA), () -> crearRegistro(bloqueB));

        assertThat(result).containsExactlyInAnyOrder(true, false);
        assertThat(usoPlanRepository.contarRegistros(NegocioId.de(negocioId))).isEqualTo(1000);
    }

    @Test
    @DisplayName("Un lote de filas que supera la cuota se rechaza completo")
    void validarAltaRegistrosShouldRejectBatchOverQuota() {
        UUID bloqueId = givenBloque("Productos");
        givenRegistros(bloqueId, 999);

        assertThatThrownBy(() -> controlPlanService.validarAltaRegistros(NegocioId.de(negocioId), 2, AHORA))
                .isInstanceOf(RestriccionPlanException.class);

        assertThat(usoPlanRepository.contarRegistros(NegocioId.de(negocioId))).isEqualTo(999);
    }

    @Test
    @DisplayName("Superar una cuota no borra ni impide consultar, renombrar y editar los datos existentes")
    void downgradeShouldPreserveDataAndBasicEditing() {
        UUID bloqueId = givenBloque("Productos");
        givenRegistros(bloqueId, 1001);
        UUID registroId = jdbcTemplate.queryForObject(
                "SELECT id FROM registro WHERE negocio_id = ? ORDER BY id LIMIT 1", UUID.class, negocioId);
        Registro registro = registroRepository.obtener(RegistroId.de(registroId), NegocioId.de(negocioId));

        registroDatosService.editar(registro, List.of(), Map.of(), AHORA.plusHours(1));

        assertThat(usoPlanRepository.contarRegistros(NegocioId.de(negocioId))).isEqualTo(1001);
        assertThat(proyectoRepository.findAllByNegocioId(NegocioId.de(negocioId))).hasSize(1);
        assertThatThrownBy(() -> crearRegistro(bloqueId)).isInstanceOf(RestriccionPlanException.class);
        UUID proyectoId = proyectoRepository.findAllByNegocioId(NegocioId.de(negocioId)).getFirst().getId().valor();
        assertThatCode(() -> renombrar(negocioId, proyectoId, "Conservado"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Las lecturas inmediatas y los lotes comparten una única cuota mensual")
    void reservarShouldSerializeImmediateAndQueuedReadings() throws Exception {
        givenConsumo(LocalDate.parse("2026-10-18"), 8, 0);
        LoteTrabajo lote = givenLoteReclamado();

        List<Boolean> result = concurrently(
                () -> reservar(lote, 2, 100, AHORA),
                this::registrarPaginaInmediata);

        assertThat(result).containsExactlyInAnyOrder(true, false);
        long total = usoPlanRepository.paginasEnPeriodo(NegocioId.de(negocioId),
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-11-01"));
        assertThat(total).isBetween(9L, 10L);
    }

    @Test
    @DisplayName("Dos lecturas inmediatas concurrentes no superan las diez páginas mensuales")
    void registrarInmediataShouldEnforceMonthlyQuotaConcurrently() throws Exception {
        givenConsumo(LocalDate.parse("2026-10-18"), 9, 0);

        List<Boolean> result = concurrently(this::registrarPaginaInmediata, this::registrarPaginaInmediata);

        assertThat(result).containsExactlyInAnyOrder(true, false);
        assertThat(usoPlanRepository.paginasEnPeriodo(NegocioId.de(negocioId),
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-11-01"))).isEqualTo(10);
    }

    @Test
    @DisplayName("Las páginas reservadas de otros días ya consumen disponibilidad mensual")
    void validarPaginasIaShouldIncludeOutstandingReservations() {
        givenConsumo(LocalDate.parse("2026-10-18"), 7, 3);

        assertThatThrownBy(this::registrarPaginaInmediata).isInstanceOf(RestriccionPlanException.class);

        assertThat(usoPlanRepository.paginasEnPeriodo(NegocioId.de(negocioId),
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-11-01"))).isEqualTo(10);
    }

    @Test
    @DisplayName("La cuota se renueva al cambiar de mes UTC sin borrar el historial")
    void registrarInmediataShouldResetAllowanceAtUtcMonthBoundary() {
        givenConsumo(LocalDate.parse("2026-10-31"), 10, 0);

        consumoIaInmediatoService.registrar(NegocioId.de(negocioId), OffsetDateTime.parse("2026-10-31T23:30:00-03:00"));

        assertThat(usoPlanRepository.paginasEnPeriodo(NegocioId.de(negocioId),
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-11-01"))).isEqualTo(10);
        assertThat(usoPlanRepository.paginasEnPeriodo(NegocioId.de(negocioId),
                LocalDate.parse("2026-11-01"), LocalDate.parse("2026-12-01"))).isEqualTo(1);
    }

    @Test
    @DisplayName("Finalizar después de medianoche conserva el mes en que se reservó la lectura")
    void finalizarShouldKeepOriginalReservationMonth() {
        LoteTrabajo lote = givenLoteReclamado();
        OffsetDateTime octubre = OffsetDateTime.parse("2026-10-31T23:59:00Z");
        assertThat(reservar(lote, 2, 100, octubre)).isTrue();

        procesamientoRepository.finalizar(lote, new ResultadoIntento(null, "Timeout", false),
                null, octubre.plusMinutes(2), null);

        assertThat(usoPlanRepository.paginasEnPeriodo(NegocioId.de(negocioId),
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-11-01"))).isEqualTo(2);
        assertThat(usoPlanRepository.paginasEnPeriodo(NegocioId.de(negocioId),
                LocalDate.parse("2026-11-01"), LocalDate.parse("2026-12-01"))).isZero();
    }

    @Test
    @DisplayName("Una reserva diaria rechazada no gasta páginas del mes")
    void reservarShouldNotChargeWhenDailyReservationFails() {
        LoteTrabajo lote = givenLoteReclamado();

        boolean result = reservar(lote, 2, 1, AHORA);

        assertThat(result).isFalse();
        assertThat(usoPlanRepository.paginasEnPeriodo(NegocioId.de(negocioId),
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-11-01"))).isZero();
    }

    @Test
    @DisplayName("El consumo inmediato cuenta páginas sin presentar un costo desconocido como cero")
    void registrarInmediataShouldExposeIncompleteTokenAccounting() {
        consumoIaInmediatoService.registrar(NegocioId.de(negocioId), AHORA);

        var consumo = procesamientoRepository.consumo(NegocioId.de(negocioId), AHORA.toLocalDate());

        assertThat(consumo.paginas()).isEqualTo(1);
        assertThat(consumo.reservadas()).isZero();
        assertThat(consumo.costoEstimadoUsd()).isNull();
        assertThat(consumo.consumoCompleto()).isFalse();
    }

    @Test
    @DisplayName("Una capacidad diaria agotada no registra una página inmediata")
    void registrarPaginaInmediataShouldRejectZeroDailyAllowance() {
        boolean result = usoPlanRepository
                .registrarPaginaInmediata(NegocioId.de(negocioId), AHORA.toLocalDate(), 0, AHORA);

        assertThat(result).isFalse();
        assertThat(usoPlanRepository.paginasEnPeriodo(NegocioId.de(negocioId),
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-11-01"))).isZero();
    }

    @Test
    @DisplayName("Premium permite más sistemas y habilita las tres funciones aprobadas")
    void premiumShouldEnableFeaturesAndUnlimitedSystems() {
        givenPremium(AHORA.plusMonths(1));

        proyectosDePrueba.crear(NegocioId.de(negocioId), "Primero", null, AHORA);
        proyectosDePrueba.crear(NegocioId.de(negocioId), "Segundo", null, AHORA);

        assertThat(usoPlanRepository.contarProyectos(NegocioId.de(negocioId))).isEqualTo(2);
        for (FuncionPlan funcion : FuncionPlan.values()) {
            assertThatCode(() -> controlPlanService.validarFuncion(NegocioId.de(negocioId), funcion, AHORA))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Premium tiene cien páginas mensuales y vuelve al gratuito al vencer")
    void premiumShouldEnforceQuotaAndExpiry() {
        givenPremium(AHORA.plusDays(1));
        givenConsumo(LocalDate.parse("2026-10-18"), 99, 0);
        consumoIaInmediatoService.registrar(NegocioId.de(negocioId), AHORA);

        assertThatThrownBy(this::registrarPaginaInmediata).isInstanceOf(RestriccionPlanException.class);

        assertThatThrownBy(() -> controlPlanService.validarFuncion(
                NegocioId.de(negocioId), FuncionPlan.RECETAS_COSTOS, AHORA.plusDays(1)))
                .isInstanceOf(RestriccionPlanException.class);
    }

    @Test
    @DisplayName("Un negocio inexistente no puede bloquear ni consultar cuotas ajenas")
    void validarAltaProyectoShouldRejectMissingNegocio() {
        assertThatThrownBy(() -> controlPlanService.validarAltaProyecto(NegocioId.nuevo(), AHORA))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Negocio no encontrado");

        assertThat(planRepository.findById(PlanId.nuevo())).isEmpty();
    }

    // --- arrange ---
    private UUID givenBloque(String nombre) {
        UUID proyectoId = proyectosDePrueba.crear(NegocioId.de(negocioId), "Sistema", null, AHORA).getId().valor();
        UUID bloqueId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, ?)",
                bloqueId, negocioId, proyectoId, nombre);
        return bloqueId;
    }

    private UUID givenBloqueInSameProyecto(UUID bloqueId, String nombre) {
        UUID proyectoId = jdbcTemplate.queryForObject(
                "SELECT proyecto_id FROM bloque WHERE id = ?", UUID.class, bloqueId);
        UUID nuevoId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, ?)",
                nuevoId, negocioId, proyectoId, nombre);
        return nuevoId;
    }

    private void givenRegistros(UUID bloqueId, int cantidad) {
        jdbcTemplate.update("""
                INSERT INTO registro (negocio_id, bloque_id, valores)
                SELECT ?, ?, '{}'::jsonb FROM generate_series(1, ?)
                """, negocioId, bloqueId, cantidad);
    }

    private void givenConsumo(LocalDate dia, int paginas, int reservadas) {
        jdbcTemplate.update("""
                INSERT INTO consumo_ia_diario (negocio_id, fecha, paginas, reservadas) VALUES (?, ?, ?, ?)
                """, negocioId, dia, paginas, reservadas);
    }

    private void givenPremium(OffsetDateTime fin) {
        jdbcTemplate.update("""
                UPDATE suscripcion SET plan_id = (SELECT id FROM plan WHERE codigo = 'PREMIUM'),
                    inicio_en = ?, fin_en = ? WHERE negocio_id = ?
                """, AHORA.minusDays(1), fin, negocioId);
    }

    private LoteTrabajo givenLoteReclamado() {
        UUID loteId = UUID.randomUUID();
        UUID token = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO lote_subida (id, negocio_id, estado, intentos, reclamo_id, reclamo_hasta)
                VALUES (?, ?, 'PROCESANDO', 1, ?, ?)
                """, loteId, negocioId, token, AHORA.plusMinutes(5));
        return new LoteTrabajo(LoteId.de(loteId), NegocioId.de(negocioId), null, ReclamoId.de(token), 1);
    }

    // --- act ---
    private boolean crearProyecto(String nombre) {
        proyectosDePrueba.crear(NegocioId.de(negocioId), nombre, null, AHORA);
        return true;
    }

    private boolean crearRegistro(UUID bloqueId) {
        crearRegistro.execute(new CrearRegistroRequest(UsuarioId.de(usuarioId), BloqueId.de(bloqueId), Map.of()));
        return true;
    }

    private boolean reservar(LoteTrabajo lote, int paginas, int limiteDiario, OffsetDateTime ahora) {
        return controlPlanService.reservarPaginasIa(lote.negocioId(), paginas, ahora,
                () -> procesamientoRepository.reservar(lote, paginas, limiteDiario, ahora));
    }

    private boolean registrarPaginaInmediata() {
        consumoIaInmediatoService.registrar(NegocioId.de(negocioId), AHORA);
        return true;
    }

    private List<Boolean> concurrently(Callable<Boolean> primera, Callable<Boolean> segunda) throws Exception {
        CountDownLatch listas = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> ejecutar(primera, listas, salida));
            var b = executor.submit(() -> ejecutar(segunda, listas, salida));
            assertThat(listas.await(10, TimeUnit.SECONDS)).isTrue();
            salida.countDown();
            return List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        }
    }

    private boolean ejecutar(Callable<Boolean> accion, CountDownLatch listas, CountDownLatch salida) throws Exception {
        listas.countDown();
        if (!salida.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("No se liberó el inicio concurrente");
        }
        try {
            return accion.call();
        } catch (RestriccionPlanException e) {
            return false;
        }
    }
    /** Editar lo que ya existe no consume cupo del plan. */
    private void renombrar(UUID negocioId, UUID proyectoId, String nombre) {
        Proyecto proyecto = proyectoRepository.obtener(NegocioId.de(negocioId), ProyectoId.de(proyectoId));
        proyecto.renombrar(nombre, AHORA);
        proyectoRepository.save(proyecto);
    }
}
