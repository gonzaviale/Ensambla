package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.model.procesamiento.ReclamoId;
import com.ensambla.backend.domain.model.procesamiento.ResultadoIntento;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.support.LecturasInmediatasDePrueba;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.ProyectosDePrueba;
import com.ensambla.backend.support.PruebasPremiumDePrueba;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PruebaPremiumPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime INICIO = OffsetDateTime.parse("2026-10-29T12:30:00Z");
    private static final String RESTRICCION_DE_PRUEBA = "ck_ens96_rollback";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PruebasPremiumDePrueba pruebaPremiumService;

    @Autowired
    private PruebaPremiumRepository pruebaPremiumRepository;

    @Autowired
    private SuscripcionRepository suscripcionRepository;

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private ControlPlanService controlPlanService;

    @Autowired
    private LecturasInmediatasDePrueba consumoIaInmediatoService;

    @Autowired
    private ProcesamientoRepository procesamientoRepository;

    @Autowired
    private ProyectosDePrueba proyectosDePrueba;

    @Autowired
    private ProyectoRepository proyectoRepository;

    private UsuarioId usuarioId;
    private NegocioId negocioId;
    private LoteTrabajo lote;
    private List<Boolean> resultados;
    private boolean resultado;
    private Throwable rechazo;
    private boolean restriccionAgregada;

    @BeforeEach
    void setUp() {
        usuarioId = UsuarioId.nuevo();
        negocioId = NegocioId.nuevo();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Prueba premium')",
                usuarioId.valor(), usuarioId.valor() + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Prueba premium', ?)",
                negocioId.valor(), usuarioId.valor());
        suscripcionService.asignarGratuito(negocioId, INICIO.minusMonths(1));
    }

    @AfterEach
    void tearDown() {
        if (restriccionAgregada) {
            jdbcTemplate.execute("ALTER TABLE suscripcion DROP CONSTRAINT " + RESTRICCION_DE_PRUEBA);
        }
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId.valor());
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioId.valor());
    }

    @Test
    @DisplayName("Dos inicios simultáneos crean una única prueba sin extender el plazo")
    void dosIniciosSimultaneosCreanUnaUnicaPruebaSinExtenderElPlazo() throws Exception {
        cuandoSeIniciaLaPruebaDosVecesAlaVez();

        entoncesSoloUnInicioTieneEfectoYElPlazoSigueEnSieteDias();
    }

    @Test
    @DisplayName("El consumo anterior del mismo día no reduce las diez páginas de prueba")
    void elConsumoAnteriorDelMismoDiaNoReduceLasDiezPaginasDePrueba() {
        dadoUnConsumoAnteriorDelMismoDia(10);
        dadoUnaPruebaIniciada();

        cuandoSeRegistraUnaLecturaInmediata(INICIO.plusSeconds(1));

        entoncesLaPruebaCobroUnaPaginaYElDiaSumaOnce();
    }

    @Test
    @DisplayName("El mes nuevo no renueva las diez páginas totales de la prueba")
    void elMesNuevoNoRenuevaLasDiezPaginasTotalesDeLaPrueba() {
        dadoUnaPruebaIniciada();
        dadoQueLaPruebaUso(9);
        dadoUnaLecturaInmediataRegistrada(INICIO.plusDays(3));

        cuandoSeIntentaRegistrarUnaLecturaInmediata(INICIO.plusDays(4));

        entoncesSeRechazaPor("LIMITE_IA_PRUEBA");
        entoncesLaPruebaUsoPaginas(10);
    }

    @Test
    @DisplayName("Lectura inmediata y reserva simultáneas comparten las últimas páginas de prueba")
    void lecturaInmediataYReservaSimultaneasComparteanLasUltimasPaginasDePrueba() throws Exception {
        dadoUnaPruebaIniciada();
        dadoQueLaPruebaUso(8);
        dadoUnLoteProcesandose();

        cuandoSeReservaYSeRegistraAlaVez();

        entoncesSoloUnaDeLasDosTieneEfectoYLaPruebaUsaNueveODiez();
    }

    @Test
    @DisplayName("Dos lecturas inmediatas simultáneas no admiten una undécima página")
    void dosLecturasInmediatasSimultaneasNoAdmitenUnaUndecimaPagina() throws Exception {
        dadoUnaPruebaIniciada();
        dadoQueLaPruebaUso(9);

        cuandoSeRegistranDosLecturasInmediatasAlaVez();

        entoncesSoloUnaTieneEfectoYLaPruebaUsaDiezPaginas();
    }

    @Test
    @DisplayName("Una reserva rechazada por el límite diario no consume páginas de prueba")
    void unaReservaRechazadaPorElLimiteDiarioNoConsumePaginasDePrueba() {
        dadoUnaPruebaIniciada();
        dadoUnLoteProcesandose();

        cuandoSeReservanDosPaginasConLimiteDiarioDeUna();

        entoncesLaReservaSeRechazaSinCobrarLaPrueba();
    }

    @Test
    @DisplayName("Una reserva duplicada del mismo reclamo no vuelve a cobrar la prueba")
    void unaReservaDuplicadaDelMismoReclamoNoVuelveACobrarLaPrueba() {
        dadoUnaPruebaIniciada();
        dadoUnLoteProcesandose();
        dadoUnaReservaDeDosPaginas();

        cuandoSeRepiteLaReservaDelMismoReclamo();

        entoncesLaReservaRepetidaSeRechazaYLaPruebaSigueEnDos();
    }

    @Test
    @DisplayName("Finalizar tarde un timeout conserva el cargo original sin duplicarlo")
    void finalizarTardeUnTimeoutConservaElCargoOriginalSinDuplicarlo() {
        dadoUnaPruebaIniciada();
        dadoUnLoteProcesandose();
        dadoUnaReservaDeDosPaginasAlSextoDia();
        dadoQueLaPruebaVencio();

        cuandoSeFinalizaElLoteConTimeoutTarde();

        entoncesSeConservaElCargoYLaSuscripcionQuedaActiva();
    }

    @ParameterizedTest
    @EnumSource(FuncionPlan.class)
    @DisplayName("La vigencia se comprueba al operar aunque todavía no haya ejecutado el job")
    void laVigenciaSeCompruebaAlOperarAunqueTodaviaNoHayaEjecutadoElJob(FuncionPlan funcion) {
        dadoUnaPruebaIniciada();

        cuandoSeValidaLaFuncion(funcion, INICIO.plusDays(7).minusNanos(1));

        entoncesLaFuncionEstaPermitida();
    }

    @Test
    @DisplayName("La prueba no permite funciones premium justo al vencer aunque el job no haya corrido")
    void laPruebaNoPermiteFuncionesPremiumJustoAlVencerAunqueElJobNoHayaCorrido() {
        dadoUnaPruebaIniciada();

        cuandoSeValidaLaFuncion(FuncionPlan.RECETAS_COSTOS, INICIO.plusDays(7));

        entoncesSeRechazaLaFuncionPorRestriccionDePlan();
    }

    @Test
    @DisplayName("Al agotar IA siguen disponibles las otras funciones premium hasta vencer")
    void alAgotarIaSiguenDisponiblesLasOtrasFuncionesPremiumHastaVencer() {
        dadoUnaPruebaIniciada();
        dadoQueLaPruebaUso(10);

        cuandoSeValidaLaFuncion(FuncionPlan.RECETAS_COSTOS, INICIO.plusDays(1));

        entoncesLaFuncionEstaPermitida();
    }

    @Test
    @DisplayName("El job vence pruebas de forma repetible y conserva sistemas y filas por encima del gratuito")
    void elJobVencePruebasDeFormaRepetibleYConservaSistemasYFilasPorEncimaDelGratuito() {
        dadoUnaPruebaIniciada();
        dadoDosSistemasYMilUnaFilas();

        cuandoElJobVenceLaPruebaDosVeces();

        entoncesLaSuscripcionVuelveAlGratuito();
        entoncesSeConservanLosSistemasYLasFilas();
        entoncesNoSePuedeCrearUnTercerSistemaNiRepetirLaPrueba();
    }

    @Test
    @DisplayName("Un premium activado después de seleccionar el vencimiento se conserva")
    void unPremiumActivadoDespuesDeSeleccionarElVencimientoSeConserva() {
        dadoUnaPruebaIniciada();
        dadoQueElNegocioPagoPremiumDespuesDeSeleccionarElVencimiento();

        cuandoElJobVenceLaPrueba();

        entoncesElPremiumPagoSeConserva();
    }

    @Test
    @DisplayName("La consulta de prueba no devuelve el historial de otro negocio")
    void laConsultaDePruebaNoDevuelveElHistorialDeOtroNegocio() {
        dadoUnaPruebaIniciada();

        entoncesOtroNegocioNoTieneHistorialDePrueba();
    }

    @Test
    @DisplayName("La base rechaza contadores mayores a diez")
    void laBaseRechazaContadoresMayoresADiez() {
        dadoUnaPruebaIniciada();

        cuandoSeIntentaGuardarQueLaPruebaUso(11);

        entoncesLaBaseRechazaElDato();
    }

    @Test
    @DisplayName("La base rechaza una segunda prueba del mismo negocio")
    void laBaseRechazaUnaSegundaPruebaDelMismoNegocio() {
        dadoUnaPruebaIniciada();

        cuandoSeIntentaInsertarUnaSegundaPrueba();

        entoncesLaBaseRechazaElDato();
    }

    @Test
    @DisplayName("La base rechaza pruebas con una duración distinta de siete días")
    void laBaseRechazaPruebasConUnaDuracionDistintaDeSieteDias() {
        dadoUnaPruebaIniciada();

        cuandoSeIntentaExtenderLaPruebaAOchoDias();

        entoncesLaBaseRechazaElDato();
    }

    @Test
    @DisplayName("Una suscripción en prueba no puede carecer de fecha de fin")
    void unaSuscripcionEnPruebaNoPuedeCarecerDeFechaDeFin() {
        dadoUnaPruebaIniciada();

        cuandoSeIntentaQuitarElFinDeLaSuscripcion();

        entoncesLaBaseRechazaElDato();
    }

    @Test
    @DisplayName("La eliminación del negocio elimina su prueba sin dejar filas huérfanas")
    void laEliminacionDelNegocioEliminaSuPruebaSinDejarFilasHuerfanas() {
        dadoUnaPruebaIniciada();

        cuandoSeEliminaElNegocio();

        entoncesLaPruebaDesaparece();
    }

    @Test
    @DisplayName("Si falla la activación se revierte también el historial que permitiría perder la prueba")
    void siFallaLaActivacionSeRevierteTambienElHistorialQuePermitiriaPerderLaPrueba() {
        dadoQueLaBaseRechazaLaSuscripcionEnPrueba();

        cuandoSeIntentaIniciarLaPrueba();

        entoncesSeRevierteElHistorialYLaSuscripcionSigueActiva();
    }

    @Test
    @DisplayName("Rechazar una lectura inmediata por cuota diaria no descuenta la prueba")
    void rechazarUnaLecturaInmediataPorCuotaDiariaNoDescuentaLaPrueba() {
        dadoUnaPruebaIniciada();
        dadoUnConsumoAnteriorDelMismoDia(100);

        cuandoSeIntentaRegistrarUnaLecturaInmediata(INICIO);

        entoncesSeRechazaPor("LIMITE_IA_DIARIO");
        entoncesLaPruebaUsoPaginas(0);
    }

    // --- dado ---

    private void dadoUnaPruebaIniciada() {
        pruebaPremiumService.iniciar(negocioId, INICIO);
    }

    private void dadoUnConsumoAnteriorDelMismoDia(int paginas) {
        jdbcTemplate.update("INSERT INTO consumo_ia_diario (negocio_id, fecha, paginas) VALUES (?, ?, ?)",
                negocioId.valor(), INICIO.toLocalDate(), paginas);
    }

    private void dadoQueLaPruebaUso(int paginas) {
        jdbcTemplate.update("UPDATE prueba_premium SET paginas_ia = ? WHERE negocio_id = ?", paginas,
                negocioId.valor());
    }

    private void dadoUnaLecturaInmediataRegistrada(OffsetDateTime instante) {
        consumoIaInmediatoService.registrar(negocioId, instante);
    }

    private void dadoUnLoteProcesandose() {
        LoteId loteId = LoteId.nuevo();
        UUID token = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO lote_subida (id, negocio_id, estado, intentos, reclamo_id, reclamo_hasta)
                VALUES (?, ?, 'PROCESANDO', 1, ?, ?)
                """, loteId.valor(), negocioId.valor(), token, INICIO.plusDays(7));
        lote = new LoteTrabajo(loteId, negocioId, null, ReclamoId.de(token), 1);
    }

    private void dadoUnaReservaDeDosPaginas() {
        resultado = reservar(lote, 2, 100, INICIO);
        assertThat(resultado).isTrue();
    }

    private void dadoUnaReservaDeDosPaginasAlSextoDia() {
        assertThat(reservar(lote, 2, 100, INICIO.plusDays(6))).isTrue();
    }

    private void dadoQueLaPruebaVencio() {
        pruebaPremiumService.vencer(negocioId, INICIO.plusDays(7));
    }

    private void dadoDosSistemasYMilUnaFilas() {
        ProyectoId proyectoId = proyectosDePrueba.crear(negocioId, "Primero", null, INICIO).getId();
        proyectosDePrueba.crear(negocioId, "Segundo", null, INICIO);
        BloqueId bloqueId = BloqueId.nuevo();
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, 'Datos')",
                bloqueId.valor(), negocioId.valor(), proyectoId.valor());
        jdbcTemplate.update("""
                INSERT INTO registro (negocio_id, bloque_id, valores)
                SELECT ?, ?, '{}'::jsonb FROM generate_series(1, 1001)
                """, negocioId.valor(), bloqueId.valor());
    }

    private void dadoQueElNegocioPagoPremiumDespuesDeSeleccionarElVencimiento() {
        assertThat(pruebaPremiumRepository.findNegociosVencidos(INICIO.plusDays(7))).contains(negocioId);
        jdbcTemplate.update("UPDATE suscripcion SET estado = 'ACTIVA', fin_en = ? WHERE negocio_id = ?",
                INICIO.plusMonths(1), negocioId.valor());
    }

    private void dadoQueLaBaseRechazaLaSuscripcionEnPrueba() {
        jdbcTemplate.execute("ALTER TABLE suscripcion ADD CONSTRAINT " + RESTRICCION_DE_PRUEBA + " "
                + "CHECK (negocio_id <> '" + negocioId.valor() + "'::uuid OR estado <> 'PRUEBA')");
        restriccionAgregada = true;
    }

    // --- cuando ---

    private void cuandoSeIniciaLaPruebaDosVecesAlaVez() throws Exception {
        resultados = concurrentemente(() -> iniciar(INICIO), () -> iniciar(INICIO.plusHours(1)));
    }

    private void cuandoSeRegistraUnaLecturaInmediata(OffsetDateTime instante) {
        consumoIaInmediatoService.registrar(negocioId, instante);
    }

    private void cuandoSeIntentaRegistrarUnaLecturaInmediata(OffsetDateTime instante) {
        rechazo = catchThrowable(() -> consumoIaInmediatoService.registrar(negocioId, instante));
    }

    private void cuandoSeReservaYSeRegistraAlaVez() throws Exception {
        resultados = concurrentemente(() -> reservar(lote, 2, 100, INICIO), () -> registrar(INICIO));
    }

    private void cuandoSeRegistranDosLecturasInmediatasAlaVez() throws Exception {
        resultados = concurrentemente(() -> registrar(INICIO), () -> registrar(INICIO));
    }

    private void cuandoSeReservanDosPaginasConLimiteDiarioDeUna() {
        resultado = reservar(lote, 2, 1, INICIO);
    }

    private void cuandoSeRepiteLaReservaDelMismoReclamo() {
        resultado = reservar(lote, 2, 100, INICIO);
    }

    private void cuandoSeFinalizaElLoteConTimeoutTarde() {
        procesamientoRepository.finalizar(lote, new ResultadoIntento(null, "Timeout", false),
                null, INICIO.plusDays(8), null);
    }

    private void cuandoSeValidaLaFuncion(FuncionPlan funcion, OffsetDateTime instante) {
        rechazo = catchThrowable(() -> controlPlanService.validarFuncion(negocioId, funcion, instante));
    }

    private void cuandoElJobVenceLaPrueba() {
        pruebaPremiumService.vencer(negocioId, INICIO.plusDays(7));
    }

    private void cuandoElJobVenceLaPruebaDosVeces() {
        cuandoElJobVenceLaPrueba();
        cuandoElJobVenceLaPrueba();
    }

    private void cuandoSeIntentaGuardarQueLaPruebaUso(int paginas) {
        rechazo = catchThrowable(() -> dadoQueLaPruebaUso(paginas));
    }

    private void cuandoSeIntentaInsertarUnaSegundaPrueba() {
        rechazo = catchThrowable(() -> jdbcTemplate.update("""
                INSERT INTO prueba_premium (negocio_id, inicio_en, fin_en) VALUES (?, ?, ?)
                """, negocioId.valor(), INICIO, INICIO.plusDays(7)));
    }

    private void cuandoSeIntentaExtenderLaPruebaAOchoDias() {
        rechazo = catchThrowable(() -> jdbcTemplate.update(
                "UPDATE prueba_premium SET fin_en = ? WHERE negocio_id = ?", INICIO.plusDays(8), negocioId.valor()));
    }

    private void cuandoSeIntentaQuitarElFinDeLaSuscripcion() {
        rechazo = catchThrowable(() -> jdbcTemplate.update(
                "UPDATE suscripcion SET fin_en = NULL WHERE negocio_id = ?", negocioId.valor()));
    }

    private void cuandoSeEliminaElNegocio() {
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId.valor());
    }

    private void cuandoSeIntentaIniciarLaPrueba() {
        rechazo = catchThrowable(() -> pruebaPremiumService.iniciar(negocioId, INICIO));
    }

    // --- entonces ---

    private void entoncesSoloUnInicioTieneEfectoYElPlazoSigueEnSieteDias() {
        assertThat(resultados).containsExactlyInAnyOrder(true, false);
        PruebaPremium prueba = prueba();
        assertThat(prueba.finEn()).isEqualTo(prueba.inicioEn().plusDays(7));
        assertThat(prueba.paginasIa()).isZero();
        assertThat(suscripcionRepository.findByNegocioId(negocioId).orElseThrow().getEstado())
                .isEqualTo(EstadoSuscripcion.PRUEBA);
    }

    private void entoncesLaPruebaCobroUnaPaginaYElDiaSumaOnce() {
        assertThat(prueba().paginasIa()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT sum(paginas) FROM consumo_ia_diario WHERE negocio_id = ?",
                Long.class, negocioId.valor())).isEqualTo(11);
    }

    private void entoncesSeRechazaPor(String codigo) {
        assertThat(rechazo).isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo(codigo);
    }

    private void entoncesLaPruebaUsoPaginas(int paginas) {
        assertThat(prueba().paginasIa()).isEqualTo(paginas);
    }

    private void entoncesSoloUnaDeLasDosTieneEfectoYLaPruebaUsaNueveODiez() {
        assertThat(resultados).containsExactlyInAnyOrder(true, false);
        assertThat(prueba().paginasIa()).isBetween(9, 10);
    }

    private void entoncesSoloUnaTieneEfectoYLaPruebaUsaDiezPaginas() {
        assertThat(resultados).containsExactlyInAnyOrder(true, false);
        assertThat(prueba().paginasIa()).isEqualTo(10);
    }

    private void entoncesLaReservaSeRechazaSinCobrarLaPrueba() {
        assertThat(resultado).isFalse();
        assertThat(prueba().paginasIa()).isZero();
    }

    private void entoncesLaReservaRepetidaSeRechazaYLaPruebaSigueEnDos() {
        assertThat(resultado).isFalse();
        assertThat(prueba().paginasIa()).isEqualTo(2);
    }

    private void entoncesSeConservaElCargoYLaSuscripcionQuedaActiva() {
        assertThat(prueba().paginasIa()).isEqualTo(2);
        assertThat(suscripcionRepository.findByNegocioId(negocioId).orElseThrow().getEstado())
                .isEqualTo(EstadoSuscripcion.ACTIVA);
    }

    private void entoncesLaFuncionEstaPermitida() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeRechazaLaFuncionPorRestriccionDePlan() {
        assertThat(rechazo).isInstanceOf(RestriccionPlanException.class);
    }

    private void entoncesLaSuscripcionVuelveAlGratuito() {
        assertThat(suscripcionRepository.findByNegocioId(negocioId).orElseThrow().getEstado())
                .isEqualTo(EstadoSuscripcion.ACTIVA);
    }

    private void entoncesSeConservanLosSistemasYLasFilas() {
        assertThat(proyectoRepository.findAllByNegocioId(negocioId)).hasSize(2);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM registro WHERE negocio_id = ?",
                Long.class, negocioId.valor())).isEqualTo(1001);
    }

    private void entoncesNoSePuedeCrearUnTercerSistemaNiRepetirLaPrueba() {
        assertThat(catchThrowable(() -> proyectosDePrueba.crear(negocioId, "Tercero", null, INICIO.plusDays(7))))
                .isInstanceOf(RestriccionPlanException.class);
        assertThat(catchThrowable(() -> pruebaPremiumService.iniciar(negocioId, INICIO.plusDays(8))))
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("PRUEBA_UTILIZADA");
        assertThat(prueba().inicioEn()).isEqualTo(INICIO);
    }

    private void entoncesElPremiumPagoSeConserva() {
        assertThat(catchThrowable(() -> controlPlanService.validarFuncion(
                negocioId, FuncionPlan.RECETAS_COSTOS, INICIO.plusDays(8)))).isNull();
        assertThat(pruebaPremiumRepository.findNegociosVencidos(INICIO.plusDays(7))).doesNotContain(negocioId);
    }

    private void entoncesOtroNegocioNoTieneHistorialDePrueba() {
        assertThat(pruebaPremiumRepository.findByNegocioId(NegocioId.nuevo())).isEmpty();
    }

    private void entoncesLaBaseRechazaElDato() {
        assertThat(rechazo).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void entoncesLaPruebaDesaparece() {
        assertThat(pruebaPremiumRepository.findByNegocioId(negocioId)).isEmpty();
    }

    private void entoncesSeRevierteElHistorialYLaSuscripcionSigueActiva() {
        assertThat(rechazo).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(pruebaPremiumRepository.findByNegocioId(negocioId)).isEmpty();
        assertThat(suscripcionRepository.findByNegocioId(negocioId).orElseThrow().getEstado())
                .isEqualTo(EstadoSuscripcion.ACTIVA);
    }

    // --- datos ---

    private boolean reservar(LoteTrabajo loteAReservar, int paginas, int limiteDiario, OffsetDateTime ahora) {
        return controlPlanService.reservarPaginasIa(loteAReservar.negocioId(), paginas, ahora,
                () -> procesamientoRepository.reservar(loteAReservar, paginas, limiteDiario, ahora));
    }

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

    private List<Boolean> concurrentemente(Callable<Boolean> primera, Callable<Boolean> segunda) throws Exception {
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
