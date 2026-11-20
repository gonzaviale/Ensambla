package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.plan.PlanId;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ControlPlanPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final LocalDate OCTUBRE = LocalDate.parse("2026-10-01");
    private static final LocalDate NOVIEMBRE = LocalDate.parse("2026-11-01");
    private static final LocalDate DICIEMBRE = LocalDate.parse("2026-12-01");
    private static final LocalDate AYER = LocalDate.parse("2026-10-18");
    private static final LocalDate ULTIMO_DIA_DE_OCTUBRE = LocalDate.parse("2026-10-31");
    private static final OffsetDateTime MEDIA_HORA_DESPUES_DE_MEDIANOCHE_DE_NOVIEMBRE =
            OffsetDateTime.parse("2026-11-01T00:30:00-03:00");
    private static final OffsetDateTime ULTIMO_MINUTO_DE_OCTUBRE = OffsetDateTime.parse("2026-11-01T02:59:00Z");
    private static final int PAGINAS_PARA_LLENAR_EL_GRATUITO = 9;

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
    private UUID bloqueA;
    private UUID bloqueB;
    private UUID otroNegocio;
    private LoteTrabajo lote;
    private List<Boolean> resultados;
    private boolean reservada;
    private long objetivosDelMes;
    private ConsumoDiario consumo;
    private Throwable error;
    private UUID objetivoRegistrado;

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
        if (otroNegocio != null) {
            borrarNegocio(otroNegocio);
        }
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId);
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioId);
    }

    @Test
    @DisplayName("Dos altas simultáneas de sistemas gratuitos permiten exactamente una")
    void dosAltasSimultaneasDeSistemasGratuitosPermitenExactamenteUna() throws Exception {
        cuandoSeCreanDosProyectosAlMismoTiempo();

        entoncesSoloSePermiteUno();
        entoncesElNegocioTieneProyectos(1);
    }

    @Test
    @DisplayName("La cuota de registros es por negocio, con altas simultáneas en bloques distintos")
    void laCuotaDeRegistrosEsPorNegocioConAltasSimultaneasEnBloquesDistintos() throws Exception {
        dadoDosBloquesYNuevecientosNoventaYNueveRegistros();

        cuandoSeCreanDosRegistrosAlMismoTiempoEnBloquesDistintos();

        entoncesSoloSePermiteUno();
        entoncesElNegocioTieneRegistros(1000);
    }

    @Test
    @DisplayName("Un lote de filas que supera la cuota se rechaza completo")
    void unLoteDeFilasQueSuperaLaCuotaSeRechazaCompleto() {
        dadoUnBloqueConRegistros(999);

        cuandoSeValidaUnAltaDeRegistros(2);

        entoncesSeRechazaPorRestriccionDePlan();
        entoncesElNegocioTieneRegistros(999);
    }

    @Test
    @DisplayName("Superar una cuota no borra ni impide consultar, renombrar y editar los datos existentes")
    void superarUnaCuotaNoBorraNiImpideConsultarRenombrarYEditarLosDatosExistentes() {
        dadoUnBloqueConRegistros(1001);

        cuandoSeEditaUnRegistroExistente();

        entoncesSeConservanLosDatos();
        entoncesNoSePuedeCrearOtroRegistro();
        entoncesSePuedeRenombrarElProyecto();
    }

    @Test
    @DisplayName("Las lecturas inmediatas y los lotes comparten una única cuota mensual")
    void lasLecturasInmediatasYLosLotesCompartenUnaUnicaCuotaMensual() throws Exception {
        dadoConsumo(AYER, 8, 0);
        dadoUnLoteReclamado();

        cuandoSeReservaUnLoteYSeRegistraUnaInmediataAlMismoTiempo();

        entoncesSoloSePermiteUno();
        entoncesElMesDeOctubreSumaEntre(9L, 10L);
    }

    @Test
    @DisplayName("Dos lecturas inmediatas concurrentes no superan las diez páginas mensuales")
    void dosLecturasInmediatasConcurrentesNoSuperanLasDiezPaginasMensuales() throws Exception {
        dadoConsumo(AYER, PAGINAS_PARA_LLENAR_EL_GRATUITO, 0);

        cuandoSeRegistranDosInmediatasAlMismoTiempo();

        entoncesSoloSePermiteUno();
        entoncesElMesDeOctubreSuma(10);
    }

    @Test
    @DisplayName("Las páginas reservadas de otros días ya consumen disponibilidad mensual")
    void lasPaginasReservadasDeOtrosDiasYaConsumenDisponibilidadMensual() {
        dadoConsumo(AYER, 7, 3);

        cuandoSeRegistraUnaInmediataConError();

        entoncesSeRechazaPorRestriccionDePlan();
        entoncesElMesDeOctubreSuma(10);
    }

    @Test
    @DisplayName("La cuota se renueva al cambiar de mes argentino sin borrar el historial")
    void laCuotaSeRenuevaAlCambiarDeMesArgentinoSinBorrarElHistorial() {
        dadoConsumo(ULTIMO_DIA_DE_OCTUBRE, 10, 0);

        cuandoSeRegistraUnaInmediataEn(MEDIA_HORA_DESPUES_DE_MEDIANOCHE_DE_NOVIEMBRE);

        entoncesElMesDeOctubreSuma(10);
        entoncesElMesDeNoviembreSuma(1);
    }

    @Test
    @DisplayName("Finalizar después de medianoche conserva el mes en que se reservó la lectura")
    void finalizarDespuesDeMedianocheConservaElMesEnQueSeReservoLaLectura() {
        dadoUnLoteReclamado();
        dadoUnaReservaDeDosPaginasEn(ULTIMO_MINUTO_DE_OCTUBRE);

        cuandoSeFinalizaElLoteConTimeout(ULTIMO_MINUTO_DE_OCTUBRE.plusMinutes(2));

        entoncesElMesDeOctubreSuma(2);
        entoncesNoHayPaginasEnNoviembre();
    }

    @Test
    @DisplayName("Una reserva diaria rechazada no gasta páginas del mes")
    void unaReservaDiariaRechazadaNoGastaPaginasDelMes() {
        dadoUnLoteReclamado();

        cuandoSeReservaUnLote(2, 1, AHORA);

        entoncesLaReservaFueRechazada();
        entoncesNoHayPaginasEnOctubre();
    }

    @Test
    @DisplayName("El consumo inmediato cuenta páginas sin presentar un costo desconocido como cero")
    void elConsumoInmediatoCuentaPaginasSinPresentarUnCostoDesconocidoComoCero() {
        cuandoSeRegistraUnaInmediataEn(AHORA);
        cuandoSeLeeElConsumoDelDia();

        entoncesElConsumoCuentaUnaPaginaConCostoDesconocido();
    }

    @Test
    @DisplayName("Una capacidad diaria agotada no registra una página inmediata")
    void unaCapacidadDiariaAgotadaNoRegistraUnaPaginaInmediata() {
        cuandoSeRegistraConCapacidadDiariaEnCero();

        entoncesLaReservaFueRechazada();
        entoncesNoHayPaginasEnOctubre();
    }

    @Test
    @DisplayName("Premium permite más de un sistema")
    void premiumPermiteMasDeUnSistema() {
        dadoPremium(AHORA.plusMonths(1));

        cuandoSeCreanDosProyectosEnSerie();

        entoncesElNegocioTieneProyectos(2);
    }

    @ParameterizedTest
    @EnumSource(FuncionPlan.class)
    @DisplayName("Premium habilita las funciones aprobadas")
    void premiumHabilitaLasFuncionesAprobadas(FuncionPlan funcion) {
        dadoPremium(AHORA.plusMonths(1));

        cuandoSeValidaLaFuncion(funcion);

        entoncesLaFuncionEstaHabilitada();
    }

    @Test
    @DisplayName("Premium tiene cien páginas mensuales y vuelve al gratuito al vencer")
    void premiumTieneCienPaginasMensualesYVuelveAlGratuitoAlVencer() {
        dadoPremium(AHORA.plusDays(1));
        dadoConsumo(AYER, 99, 0);
        dadoUnaPaginaInmediataRegistrada();

        cuandoSeRegistraUnaInmediataConError();

        entoncesSeRechazaPorRestriccionDePlan();
        entoncesPremiumYaNoHabilitaRecetasUnDiaDespues();
    }

    @Test
    @DisplayName("Un negocio inexistente no puede bloquear ni consultar cuotas ajenas")
    void unNegocioInexistenteNoPuedeBloquearNiConsultarCuotasAjenas() {
        cuandoSeValidaElAltaDeProyectoDeUnNegocioInexistente();

        entoncesFallaPorNegocioNoEncontrado();
        entoncesNoHayUnPlanConEseId();
    }

    @Test
    @DisplayName("Cuenta los objetivos con IA del mes del negocio y no los de otros meses ni los de otro negocio")
    void cuentaLosObjetivosConIaDelMesDelNegocioYNoLosDeOtrosMesesNiLosDeOtroNegocio() {
        dadoDosObjetivosEsteMesUnoElAnteriorYUnoDeOtroNegocio();

        cuandoSeCuentanLosObjetivosIaDeOctubre();

        entoncesSeCuentanDos();
    }

    @Test
    @DisplayName("El gratuito guarda cinco objetivos con IA por mes")
    void elGratuitoGuardaCincoObjetivosConIaPorMes() {
        dadoCuatroObjetivosConIa();

        cuandoSeValidaUnObjetivoIaConError();

        entoncesSeAceptaElObjetivo();
    }

    @Test
    @DisplayName("El gratuito rechaza el sexto objetivo con IA del mes con LIMITE_OBJETIVOS_IA")
    void elGratuitoRechazaElSextoObjetivoConIaDelMesConLimiteObjetivosIa() {
        dadoCincoObjetivosConIa();

        cuandoSeValidaUnObjetivoIaConError();

        entoncesSeRechazaPorLimiteDeObjetivosIa();
        entoncesNoQuedanObjetivosIa();
    }

    @Test
    @DisplayName("Borrar un objetivo no devuelve la cuota del mes")
    void borrarUnObjetivoNoDevuelveLaCuotaDelMes() {
        dadoUnObjetivoConIaRegistrado();

        cuandoSeBorraElObjetivo();

        entoncesLaCuotaSigueUsada(1);
    }

    // --- dado ---
    private void dadoDosBloquesYNuevecientosNoventaYNueveRegistros() {
        bloqueA = insertarBloque("Productos");
        bloqueB = insertarBloqueEnElMismoProyecto(bloqueA, "Ventas");
        insertarRegistros(bloqueA, 999);
    }

    private void dadoUnBloqueConRegistros(int cantidad) {
        bloqueA = insertarBloque("Productos");
        insertarRegistros(bloqueA, cantidad);
    }

    private void dadoUnLoteReclamado() {
        UUID loteId = UUID.randomUUID();
        UUID token = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO lote_subida (id, negocio_id, estado, intentos, reclamo_id, reclamo_hasta)
                VALUES (?, ?, 'PROCESANDO', 1, ?, ?)
                """, loteId, negocioId, token, AHORA.plusMinutes(5));
        lote = new LoteTrabajo(LoteId.de(loteId), NegocioId.de(negocioId), null, ReclamoId.de(token), 1);
    }

    private void dadoUnaReservaDeDosPaginasEn(OffsetDateTime momento) {
        reservar(lote, 2, 100, momento);
    }

    private void dadoUnaPaginaInmediataRegistrada() {
        consumoIaInmediatoService.registrar(NegocioId.de(negocioId), AHORA);
    }

    private void dadoConsumo(LocalDate dia, int paginas, int reservadas) {
        jdbcTemplate.update("""
                INSERT INTO consumo_ia_diario (negocio_id, fecha, paginas, reservadas) VALUES (?, ?, ?, ?)
                """, negocioId, dia, paginas, reservadas);
    }

    private void dadoPremium(OffsetDateTime fin) {
        jdbcTemplate.update("""
                UPDATE suscripcion SET plan_id = (SELECT id FROM plan WHERE codigo = 'PREMIUM'),
                    inicio_en = ?, fin_en = ? WHERE negocio_id = ?
                """, AHORA.minusDays(1), fin, negocioId);
    }

    private void dadoDosObjetivosEsteMesUnoElAnteriorYUnoDeOtroNegocio() {
        NegocioId negocio = NegocioId.de(negocioId);
        usoPlanRepository.registrarObjetivoIa(negocio, ObjetivoId.nuevo(), AHORA);
        usoPlanRepository.registrarObjetivoIa(negocio, ObjetivoId.nuevo(), AHORA.plusDays(3));
        usoPlanRepository.registrarObjetivoIa(negocio, ObjetivoId.nuevo(), AHORA.minusMonths(1));
        otroNegocio = insertarOtroNegocio();
        usoPlanRepository.registrarObjetivoIa(NegocioId.de(otroNegocio), ObjetivoId.nuevo(), AHORA);
    }

    private void dadoCuatroObjetivosConIa() {
        IntStream.range(0, 4).forEach(indice -> dadoUnObjetivoConIaMas());
    }

    private void dadoCincoObjetivosConIa() {
        dadoCuatroObjetivosConIa();
        dadoUnObjetivoConIaMas();
    }

    private void dadoUnObjetivoConIaMas() {
        usoPlanRepository.registrarObjetivoIa(NegocioId.de(negocioId), ObjetivoId.nuevo(), AHORA);
    }

    private void dadoUnObjetivoConIaRegistrado() {
        objetivoRegistrado = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO objetivo (id, negocio_id, texto, tipo, armado_con_sami) "
                + "VALUES (?, ?, 'Que me conozcan', 'SIN_DATOS', true)", objetivoRegistrado, negocioId);
        usoPlanRepository.registrarObjetivoIa(NegocioId.de(negocioId), ObjetivoId.de(objetivoRegistrado), AHORA);
    }

    // --- cuando ---
    private void cuandoSeCreanDosProyectosAlMismoTiempo() throws Exception {
        resultados = concurrently(() -> crearProyecto("Primero"), () -> crearProyecto("Segundo"));
    }

    private void cuandoSeCreanDosRegistrosAlMismoTiempoEnBloquesDistintos() throws Exception {
        resultados = concurrently(() -> crearRegistro(bloqueA), () -> crearRegistro(bloqueB));
    }

    private void cuandoSeValidaUnAltaDeRegistros(int cantidad) {
        error = catchThrowable(
                () -> controlPlanService.validarAltaRegistros(NegocioId.de(negocioId), cantidad, AHORA));
    }

    private void cuandoSeEditaUnRegistroExistente() {
        UUID registroId = jdbcTemplate.queryForObject(
                "SELECT id FROM registro WHERE negocio_id = ? ORDER BY id LIMIT 1", UUID.class, negocioId);
        Registro registro = registroRepository.obtener(RegistroId.de(registroId), NegocioId.de(negocioId));
        registroDatosService.editar(registro, List.of(), Map.of(), AHORA.plusHours(1));
    }

    private void cuandoSeReservaUnLoteYSeRegistraUnaInmediataAlMismoTiempo() throws Exception {
        resultados = concurrently(() -> reservar(lote, 2, 100, AHORA), this::registrarPaginaInmediata);
    }

    private void cuandoSeRegistranDosInmediatasAlMismoTiempo() throws Exception {
        resultados = concurrently(this::registrarPaginaInmediata, this::registrarPaginaInmediata);
    }

    private void cuandoSeRegistraUnaInmediataConError() {
        error = catchThrowable(this::registrarPaginaInmediata);
    }

    private void cuandoSeRegistraUnaInmediataEn(OffsetDateTime momento) {
        consumoIaInmediatoService.registrar(NegocioId.de(negocioId), momento);
    }

    private void cuandoSeFinalizaElLoteConTimeout(OffsetDateTime momento) {
        procesamientoRepository.finalizar(lote, new ResultadoIntento(null, "Timeout", false), null, momento, null);
    }

    private void cuandoSeReservaUnLote(int paginas, int limiteDiario, OffsetDateTime momento) {
        reservada = reservar(lote, paginas, limiteDiario, momento);
    }

    private void cuandoSeLeeElConsumoDelDia() {
        consumo = procesamientoRepository.consumo(NegocioId.de(negocioId), AHORA.toLocalDate());
    }

    private void cuandoSeRegistraConCapacidadDiariaEnCero() {
        reservada = usoPlanRepository
                .registrarPaginaInmediata(NegocioId.de(negocioId), AHORA.toLocalDate(), 0, AHORA);
    }

    private void cuandoSeCreanDosProyectosEnSerie() {
        proyectosDePrueba.crear(NegocioId.de(negocioId), "Primero", null, AHORA);
        proyectosDePrueba.crear(NegocioId.de(negocioId), "Segundo", null, AHORA);
    }

    private void cuandoSeValidaElAltaDeProyectoDeUnNegocioInexistente() {
        error = catchThrowable(() -> controlPlanService.validarAltaProyecto(NegocioId.nuevo(), AHORA));
    }

    private void cuandoSeCuentanLosObjetivosIaDeOctubre() {
        objetivosDelMes = usoPlanRepository.objetivosIaEnPeriodo(NegocioId.de(negocioId),
                OffsetDateTime.parse("2026-10-01T03:00:00Z"), OffsetDateTime.parse("2026-11-01T03:00:00Z"));
    }

    private void cuandoSeValidaUnObjetivoIaConError() {
        error = catchThrowable(() -> controlPlanService.validarObjetivoIa(NegocioId.de(negocioId), AHORA));
    }

    private void cuandoSeBorraElObjetivo() {
        jdbcTemplate.update("DELETE FROM objetivo WHERE id = ?", objetivoRegistrado);
    }

    private void cuandoSeValidaLaFuncion(FuncionPlan funcion) {
        error = catchThrowable(() -> controlPlanService.validarFuncion(NegocioId.de(negocioId), funcion, AHORA));
    }

    // --- entonces ---
    private void entoncesSoloSePermiteUno() {
        assertThat(resultados).containsExactlyInAnyOrder(true, false);
    }

    private void entoncesElNegocioTieneProyectos(int cantidad) {
        assertThat(usoPlanRepository.contarProyectos(NegocioId.de(negocioId))).isEqualTo(cantidad);
    }

    private void entoncesElNegocioTieneRegistros(int cantidad) {
        assertThat(usoPlanRepository.contarRegistros(NegocioId.de(negocioId))).isEqualTo(cantidad);
    }

    private void entoncesSeRechazaPorRestriccionDePlan() {
        assertThat(error).isInstanceOf(RestriccionPlanException.class);
    }

    private void entoncesSeConservanLosDatos() {
        entoncesElNegocioTieneRegistros(1001);
        assertThat(proyectoRepository.findAllByNegocioId(NegocioId.de(negocioId))).hasSize(1);
    }

    private void entoncesNoSePuedeCrearOtroRegistro() {
        assertThat(catchThrowable(() -> crearRegistro(bloqueA))).isInstanceOf(RestriccionPlanException.class);
    }

    private void entoncesSePuedeRenombrarElProyecto() {
        UUID proyectoId = proyectoRepository.findAllByNegocioId(NegocioId.de(negocioId)).getFirst().getId().valor();
        assertThat(catchThrowable(() -> renombrar(negocioId, proyectoId, "Conservado"))).isNull();
    }

    private void entoncesElMesDeOctubreSumaEntre(long minimo, long maximo) {
        assertThat(paginasEn(OCTUBRE, NOVIEMBRE)).isBetween(minimo, maximo);
    }

    private void entoncesElMesDeOctubreSuma(long paginas) {
        assertThat(paginasEn(OCTUBRE, NOVIEMBRE)).isEqualTo(paginas);
    }

    private void entoncesElMesDeNoviembreSuma(long paginas) {
        assertThat(paginasEn(NOVIEMBRE, DICIEMBRE)).isEqualTo(paginas);
    }

    private void entoncesNoHayPaginasEnOctubre() {
        assertThat(paginasEn(OCTUBRE, NOVIEMBRE)).isZero();
    }

    private void entoncesNoHayPaginasEnNoviembre() {
        assertThat(paginasEn(NOVIEMBRE, DICIEMBRE)).isZero();
    }

    private void entoncesLaReservaFueRechazada() {
        assertThat(reservada).isFalse();
    }

    private void entoncesElConsumoCuentaUnaPaginaConCostoDesconocido() {
        assertThat(consumo.paginas()).isEqualTo(1);
        assertThat(consumo.reservadas()).isZero();
        assertThat(consumo.costoEstimadoUsd()).isNull();
        assertThat(consumo.consumoCompleto()).isFalse();
    }

    private void entoncesLaFuncionEstaHabilitada() {
        assertThat(error).isNull();
    }

    private void entoncesPremiumYaNoHabilitaRecetasUnDiaDespues() {
        assertThat(catchThrowable(() -> controlPlanService.validarFuncion(
                NegocioId.de(negocioId), FuncionPlan.RECETAS_COSTOS, AHORA.plusDays(1))))
                .isInstanceOf(RestriccionPlanException.class);
    }

    private void entoncesFallaPorNegocioNoEncontrado() {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage("Negocio no encontrado");
    }

    private void entoncesNoHayUnPlanConEseId() {
        assertThat(planRepository.findById(PlanId.nuevo())).isEmpty();
    }

    private void entoncesSeCuentanDos() {
        assertThat(objetivosDelMes).isEqualTo(2);
    }

    private void entoncesSeAceptaElObjetivo() {
        assertThat(error).isNull();
    }

    private void entoncesSeRechazaPorLimiteDeObjetivosIa() {
        assertThat(error).isInstanceOf(RestriccionPlanException.class);
        assertThat(((RestriccionPlanException) error).getCodigo()).isEqualTo("LIMITE_OBJETIVOS_IA");
    }

    private void entoncesNoQuedanObjetivosIa() {
        assertThat(controlPlanService.cuotaObjetivosIa(NegocioId.de(negocioId), AHORA).restantes()).isZero();
    }

    private void entoncesLaCuotaSigueUsada(int usadas) {
        assertThat(controlPlanService.cuotaObjetivosIa(NegocioId.de(negocioId), AHORA).usadas()).isEqualTo(usadas);
    }

    // --- datos ---
    private long paginasEn(LocalDate desde, LocalDate hasta) {
        return usoPlanRepository.paginasEnPeriodo(NegocioId.de(negocioId), desde, hasta);
    }

    private UUID insertarBloque(String nombre) {
        UUID proyectoId = proyectosDePrueba.crear(NegocioId.de(negocioId), "Sistema", null, AHORA).getId().valor();
        UUID bloqueId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, ?)",
                bloqueId, negocioId, proyectoId, nombre);
        return bloqueId;
    }

    private UUID insertarBloqueEnElMismoProyecto(UUID bloqueId, String nombre) {
        UUID proyectoId = jdbcTemplate.queryForObject(
                "SELECT proyecto_id FROM bloque WHERE id = ?", UUID.class, bloqueId);
        UUID nuevoId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, ?)",
                nuevoId, negocioId, proyectoId, nombre);
        return nuevoId;
    }

    private void insertarRegistros(UUID bloqueId, int cantidad) {
        jdbcTemplate.update("""
                INSERT INTO registro (negocio_id, bloque_id, valores)
                SELECT ?, ?, '{}'::jsonb FROM generate_series(1, ?)
                """, negocioId, bloqueId, cantidad);
    }

    private boolean crearProyecto(String nombre) {
        proyectosDePrueba.crear(NegocioId.de(negocioId), nombre, null, AHORA);
        return true;
    }

    private boolean crearRegistro(UUID bloqueId) {
        crearRegistro.execute(new CrearRegistroRequest(UsuarioId.de(usuarioId), BloqueId.de(bloqueId), Map.of()));
        return true;
    }

    private boolean reservar(LoteTrabajo loteAReservar, int paginas, int limiteDiario, OffsetDateTime ahora) {
        return controlPlanService.reservarPaginasIa(loteAReservar.negocioId(), paginas, ahora,
                () -> procesamientoRepository.reservar(loteAReservar, paginas, limiteDiario, ahora));
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

    private void renombrar(UUID negocioDelProyecto, UUID proyectoId, String nombre) {
        Proyecto proyecto = proyectoRepository.obtener(NegocioId.de(negocioDelProyecto), ProyectoId.de(proyectoId));
        proyecto.renombrar(nombre, AHORA);
        proyectoRepository.save(proyecto);
    }

    private void borrarNegocio(UUID negocio) {
        UUID duenio = jdbcTemplate.queryForObject("SELECT duenio_id FROM negocio WHERE id = ?", UUID.class, negocio);
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocio);
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", duenio);
    }

    private UUID insertarOtroNegocio() {
        UUID otroUsuario = UUID.randomUUID();
        UUID otroNegocioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Otro')",
                otroUsuario, otroUsuario + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Otro', ?)",
                otroNegocioId, otroUsuario);
        suscripcionService.asignarGratuito(NegocioId.de(otroNegocioId), AHORA.minusDays(30));
        return otroNegocioId;
    }
}
