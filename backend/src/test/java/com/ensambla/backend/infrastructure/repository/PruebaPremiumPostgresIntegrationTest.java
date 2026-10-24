package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.model.procesamiento.ResultadoIntento;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.service.plan.ConsumoIaInmediatoService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.plan.PruebaPremiumService;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.domain.service.procesamiento.ReservaIaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.plan.VencerPruebasUseCase;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PruebaPremiumPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime INICIO = OffsetDateTime.parse("2026-10-29T12:30:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PruebaPremiumService pruebaPremiumService;

    @Autowired
    private PruebaPremiumRepository pruebaPremiumRepository;

    @Autowired
    private SuscripcionRepository suscripcionRepository;

    @Autowired
    private SuscripcionService suscripcionService;

    @Autowired
    private ControlPlanService controlPlanService;

    @Autowired
    private ConsumoIaInmediatoService consumoIaInmediatoService;

    @Autowired
    private ReservaIaService reservaIaService;

    @Autowired
    private ProcesamientoRepository procesamientoRepository;

    @Autowired
    private ProyectoService proyectoService;

    private UUID usuarioId;
    private UUID negocioId;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        negocioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Prueba premium')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Prueba premium', ?)",
                negocioId, usuarioId);
        suscripcionService.asignarGratuito(negocioId, INICIO.minusMonths(1));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId);
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioId);
    }

    @Test
    @DisplayName("Dos inicios simultáneos crean una única prueba sin extender el plazo")
    void iniciarShouldSerializeRepeatedRequests() throws Exception {
        List<Boolean> result = concurrently(() -> iniciar(INICIO), () -> iniciar(INICIO.plusHours(1)));

        assertThat(result).containsExactlyInAnyOrder(true, false);
        PruebaPremium prueba = pruebaPremiumRepository.findByNegocioId(negocioId).orElseThrow();
        assertThat(prueba.finEn()).isEqualTo(prueba.inicioEn().plusDays(7));
        assertThat(prueba.paginasIa()).isZero();
        assertThat(suscripcionRepository.findByNegocioId(negocioId).orElseThrow().getEstado())
                .isEqualTo(EstadoSuscripcion.PRUEBA);
    }

    @Test
    @DisplayName("El consumo anterior del mismo día no reduce las diez páginas de prueba")
    void iniciarShouldNotChargeEarlierFreeUsage() {
        givenConsumoAnterior(10);
        pruebaPremiumService.iniciar(negocioId, INICIO);

        consumoIaInmediatoService.registrar(negocioId, INICIO.plusSeconds(1));

        assertThat(prueba().paginasIa()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT sum(paginas) FROM consumo_ia_diario WHERE negocio_id = ?",
                Long.class, negocioId)).isEqualTo(11);
    }

    @Test
    @DisplayName("El mes nuevo no renueva las diez páginas totales de la prueba")
    void registrarShouldKeepTrialQuotaAcrossMonthBoundary() {
        pruebaPremiumService.iniciar(negocioId, INICIO);
        givenPaginasPrueba(9);
        consumoIaInmediatoService.registrar(negocioId, INICIO.plusDays(3));

        assertThatThrownBy(() -> consumoIaInmediatoService.registrar(negocioId, INICIO.plusDays(4)))
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("LIMITE_IA_PRUEBA");

        assertThat(prueba().paginasIa()).isEqualTo(10);
    }

    @Test
    @DisplayName("Lectura inmediata y reserva simultáneas comparten las últimas páginas de prueba")
    void reservarShouldSerializeQueuedAndImmediateTrialUsage() throws Exception {
        pruebaPremiumService.iniciar(negocioId, INICIO);
        givenPaginasPrueba(8);
        LoteTrabajo lote = givenLote();

        List<Boolean> result = concurrently(() -> reservaIaService.reservar(lote, 2, 100, INICIO),
                () -> registrar(INICIO));

        assertThat(result).containsExactlyInAnyOrder(true, false);
        assertThat(prueba().paginasIa()).isBetween(9, 10);
    }

    @Test
    @DisplayName("Dos lecturas inmediatas simultáneas no admiten una undécima página")
    void registrarShouldEnforceTrialQuotaConcurrently() throws Exception {
        pruebaPremiumService.iniciar(negocioId, INICIO);
        givenPaginasPrueba(9);

        List<Boolean> result = concurrently(() -> registrar(INICIO), () -> registrar(INICIO));

        assertThat(result).containsExactlyInAnyOrder(true, false);
        assertThat(prueba().paginasIa()).isEqualTo(10);
    }

    @Test
    @DisplayName("Una reserva rechazada por el límite diario no consume páginas de prueba")
    void reservarShouldNotChargeRejectedReservation() {
        pruebaPremiumService.iniciar(negocioId, INICIO);

        boolean result = reservaIaService.reservar(givenLote(), 2, 1, INICIO);

        assertThat(result).isFalse();
        assertThat(prueba().paginasIa()).isZero();
    }

    @Test
    @DisplayName("Una reserva duplicada del mismo reclamo no vuelve a cobrar la prueba")
    void reservarShouldNotChargeSameClaimTwice() {
        pruebaPremiumService.iniciar(negocioId, INICIO);
        LoteTrabajo lote = givenLote();
        assertThat(reservaIaService.reservar(lote, 2, 100, INICIO)).isTrue();

        boolean result = reservaIaService.reservar(lote, 2, 100, INICIO);

        assertThat(result).isFalse();
        assertThat(prueba().paginasIa()).isEqualTo(2);
    }

    @Test
    @DisplayName("Finalizar tarde un timeout conserva el cargo original sin duplicarlo")
    void finalizarShouldKeepChargeAfterTrialExpiry() {
        pruebaPremiumService.iniciar(negocioId, INICIO);
        LoteTrabajo lote = givenLote();
        assertThat(reservaIaService.reservar(lote, 2, 100, INICIO.plusDays(6))).isTrue();
        pruebaPremiumService.vencer(negocioId, INICIO.plusDays(7));

        procesamientoRepository.finalizar(lote, new ResultadoIntento(null, "Timeout", false),
                null, INICIO.plusDays(8), null);

        assertThat(prueba().paginasIa()).isEqualTo(2);
        assertThat(suscripcionRepository.findByNegocioId(negocioId).orElseThrow().getEstado())
                .isEqualTo(EstadoSuscripcion.ACTIVA);
    }

    @Test
    @DisplayName("La vigencia se comprueba al operar aunque todavía no haya ejecutado el job")
    void validarFuncionShouldRejectTrialAtExactExpiryWithoutJob() {
        pruebaPremiumService.iniciar(negocioId, INICIO);
        for (FuncionPlan funcion : FuncionPlan.values()) {
            assertThatCode(() -> controlPlanService.validarFuncion(
                    negocioId, funcion, INICIO.plusDays(7).minusNanos(1))).doesNotThrowAnyException();
        }

        assertThatThrownBy(() -> controlPlanService.validarFuncion(
                negocioId, FuncionPlan.RECETAS_COSTOS, INICIO.plusDays(7)))
                .isInstanceOf(RestriccionPlanException.class);
    }

    @Test
    @DisplayName("Al agotar IA siguen disponibles las otras funciones premium hasta vencer")
    void validarFuncionShouldKeepPremiumAfterAiQuotaExhaustion() {
        pruebaPremiumService.iniciar(negocioId, INICIO);
        givenPaginasPrueba(10);

        assertThatCode(() -> controlPlanService.validarFuncion(
                negocioId, FuncionPlan.RECETAS_COSTOS, INICIO.plusDays(1))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("El job vence pruebas de forma repetible y conserva sistemas y filas por encima del gratuito")
    void executeShouldExpireTrialAndPreserveData() {
        pruebaPremiumService.iniciar(negocioId, INICIO);
        UUID proyectoId = proyectoService.crear(negocioId, "Primero", null, INICIO).getId();
        proyectoService.crear(negocioId, "Segundo", null, INICIO);
        UUID bloqueId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, 'Datos')",
                bloqueId, negocioId, proyectoId);
        jdbcTemplate.update("""
                INSERT INTO registro (negocio_id, bloque_id, valores)
                SELECT ?, ?, '{}'::jsonb FROM generate_series(1, 1001)
                """, negocioId, bloqueId);
        VencerPruebasUseCase job = new VencerPruebasUseCase(pruebaPremiumService,
                Clock.fixed(INICIO.plusDays(7).toInstant(), ZoneOffset.UTC));

        job.execute();
        job.execute();

        assertThat(suscripcionRepository.findByNegocioId(negocioId).orElseThrow().getEstado())
                .isEqualTo(EstadoSuscripcion.ACTIVA);
        assertThat(proyectoService.listar(negocioId)).hasSize(2);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM registro WHERE negocio_id = ?",
                Long.class, negocioId)).isEqualTo(1001);
        assertThatThrownBy(() -> proyectoService.crear(negocioId, "Tercero", null, INICIO.plusDays(7)))
                .isInstanceOf(RestriccionPlanException.class);
        assertThatThrownBy(() -> pruebaPremiumService.iniciar(negocioId, INICIO.plusDays(8)))
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("PRUEBA_UTILIZADA");
        assertThat(prueba().inicioEn()).isEqualTo(INICIO);
    }

    @Test
    @DisplayName("Un premium activado después de seleccionar el vencimiento se conserva")
    void vencerShouldPreservePaidPremiumAfterTrial() {
        pruebaPremiumService.iniciar(negocioId, INICIO);
        assertThat(pruebaPremiumService.listarVencidas(INICIO.plusDays(7))).contains(negocioId);
        jdbcTemplate.update("UPDATE suscripcion SET estado = 'ACTIVA', fin_en = ? WHERE negocio_id = ?",
                INICIO.plusMonths(1), negocioId);

        pruebaPremiumService.vencer(negocioId, INICIO.plusDays(7));

        assertThatCode(() -> controlPlanService.validarFuncion(
                negocioId, FuncionPlan.RECETAS_COSTOS, INICIO.plusDays(8))).doesNotThrowAnyException();
        assertThat(pruebaPremiumService.listarVencidas(INICIO.plusDays(7))).doesNotContain(negocioId);
    }

    @Test
    @DisplayName("La consulta de prueba no devuelve el historial de otro negocio")
    void findByNegocioIdShouldIsolateTrialHistory() {
        pruebaPremiumService.iniciar(negocioId, INICIO);

        assertThat(pruebaPremiumRepository.findByNegocioId(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("La base rechaza contadores mayores a diez")
    void updateShouldRejectExceededTrialQuota() {
        pruebaPremiumService.iniciar(negocioId, INICIO);

        assertThatThrownBy(() -> givenPaginasPrueba(11)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza una segunda prueba del mismo negocio")
    void saveShouldRejectDuplicateTrialHistory() {
        pruebaPremiumService.iniciar(negocioId, INICIO);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO prueba_premium (negocio_id, inicio_en, fin_en) VALUES (?, ?, ?)
                """, negocioId, INICIO, INICIO.plusDays(7))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza pruebas con una duración distinta de siete días")
    void updateShouldRejectInvalidTrialDuration() {
        pruebaPremiumService.iniciar(negocioId, INICIO);

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE prueba_premium SET fin_en = ? WHERE negocio_id = ?",
                INICIO.plusDays(8), negocioId)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Una suscripción en prueba no puede carecer de fecha de fin")
    void updateShouldRejectTrialWithoutEnd() {
        pruebaPremiumService.iniciar(negocioId, INICIO);

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE suscripcion SET fin_en = NULL WHERE negocio_id = ?",
                negocioId)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La eliminación del negocio elimina su prueba sin dejar filas huérfanas")
    void deleteShouldCascadeTrialHistory() {
        pruebaPremiumService.iniciar(negocioId, INICIO);

        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId);

        assertThat(pruebaPremiumRepository.findByNegocioId(negocioId)).isEmpty();
    }

    @Test
    @DisplayName("Si falla la activación se revierte también el historial que permitiría perder la prueba")
    void iniciarShouldRollbackHistoryWhenSubscriptionUpdateFails() {
        jdbcTemplate.execute("ALTER TABLE suscripcion ADD CONSTRAINT ck_ens96_rollback "
                + "CHECK (negocio_id <> '" + negocioId + "'::uuid OR estado <> 'PRUEBA')");
        try {
            assertThatThrownBy(() -> pruebaPremiumService.iniciar(negocioId, INICIO))
                    .isInstanceOf(DataIntegrityViolationException.class);

            assertThat(pruebaPremiumRepository.findByNegocioId(negocioId)).isEmpty();
            assertThat(suscripcionRepository.findByNegocioId(negocioId).orElseThrow().getEstado())
                    .isEqualTo(EstadoSuscripcion.ACTIVA);
        } finally {
            jdbcTemplate.execute("ALTER TABLE suscripcion DROP CONSTRAINT ck_ens96_rollback");
        }
    }

    @Test
    @DisplayName("Rechazar una lectura inmediata por cuota diaria no descuenta la prueba")
    void registrarShouldNotChargeTrialWhenDailyLimitIsReached() {
        pruebaPremiumService.iniciar(negocioId, INICIO);
        givenConsumoAnterior(100);

        assertThatThrownBy(() -> consumoIaInmediatoService.registrar(negocioId, INICIO))
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("LIMITE_IA_DIARIO");

        assertThat(prueba().paginasIa()).isZero();
    }

    // --- arrange ---
    private void givenConsumoAnterior(int paginas) {
        jdbcTemplate.update("INSERT INTO consumo_ia_diario (negocio_id, fecha, paginas) VALUES (?, ?, ?)",
                negocioId, INICIO.toLocalDate(), paginas);
    }

    private void givenPaginasPrueba(int paginas) {
        jdbcTemplate.update("UPDATE prueba_premium SET paginas_ia = ? WHERE negocio_id = ?", paginas, negocioId);
    }

    private LoteTrabajo givenLote() {
        UUID loteId = UUID.randomUUID();
        UUID token = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO lote_subida (id, negocio_id, estado, intentos, reclamo_id, reclamo_hasta)
                VALUES (?, ?, 'PROCESANDO', 1, ?, ?)
                """, loteId, negocioId, token, INICIO.plusDays(7));
        return new LoteTrabajo(loteId, negocioId, null, token, 1);
    }

    // --- act ---
    private boolean iniciar(OffsetDateTime ahora) {
        pruebaPremiumService.iniciar(negocioId, ahora);
        return true;
    }

    private boolean registrar(OffsetDateTime ahora) {
        consumoIaInmediatoService.registrar(negocioId, ahora);
        return true;
    }

    private PruebaPremium prueba() {
        return pruebaPremiumRepository.findByNegocioId(negocioId).orElseThrow();
    }

    // --- helpers ---
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
}
