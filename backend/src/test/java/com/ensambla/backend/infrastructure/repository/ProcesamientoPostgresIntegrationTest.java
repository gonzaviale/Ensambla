package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;
import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.model.procesamiento.ReclamoId;
import com.ensambla.backend.domain.model.procesamiento.ResultadoIntento;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.procesamiento.procesarLote.ProcesarLote;
import com.ensambla.backend.infrastructure.ia.ArchivoCuadernoLocalAdapter;
import com.ensambla.backend.infrastructure.ia.IaProperties;
import com.ensambla.backend.infrastructure.repository.jpaRepository.implementation.ProcesamientoRepositoryImpl;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.ProcesamientoTestData;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ensambla.backend.support.ProcesamientoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;

class ProcesamientoPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    @TempDir
    private Path archivos;

    @Autowired
    private LectorCuadernoPort lectorCuadernoPort;

    @Autowired
    private ProcesamientoRepository procesamientoRepository;

    @Autowired
    private ControlPlanService controlPlanService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private UsuarioId usuarioId;
    private NegocioId negocioId;
    private LoteId primerLote;
    private LoteId segundoLote;
    private LoteId loteId;
    private LoteTrabajo lote;
    private LoteTrabajo loteViejo;
    private LoteTrabajo loteNuevo;
    private BloqueId bloqueId;
    private BloqueId clientesId;
    private BloqueId ventasId;
    private CampoId campoId;
    private ProyectoId proyectoId;
    private List<Optional<LoteTrabajo>> reclamos;
    private List<Boolean> reservas;
    private List<LoteTrabajo> trabajos;
    private List<BloqueLectura> bloques;
    private List<CampoConocido> camposDeVentas;
    private List<TablaDisponible> tablas;
    private List<LoteTrabajo> vencidos;
    private boolean resultado;

    @BeforeEach
    void setUp() {
        usuarioId = UsuarioId.nuevo();
        negocioId = NegocioId.nuevo();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Prueba IA')",
                usuarioId.valor(), usuarioId.valor() + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Prueba IA', ?)",
                negocioId.valor(), usuarioId.valor());
        suscripcionService.asignarGratuito(negocioId, AHORA.minusDays(1));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM negocio WHERE id = ?", negocioId.valor());
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioId.valor());
    }

    @Test
    @DisplayName("Dos lotes del mismo cuaderno recorren el worker real y reutilizan el bloque propio")
    void dosLotesDelMismoCuadernoRecorrenElWorkerRealYReutilizanElBloquePropio() throws Exception {
        dadoDosLotesConFotoYUnBloqueDeVentas();

        cuandoElWorkerProcesaDosVeces();

        entoncesAmbosLotesQuedanListosSobreElBloqueDeVentas();
        entoncesSeCobraUnaPaginaPorLote();
        entoncesNoSeCreaNingunRegistro();
    }

    @Test
    @DisplayName("Dos workers reclaman lotes distintos sin perder trabajo")
    void dosWorkersReclamanLotesDistintosSinPerderTrabajo() throws Exception {
        dadoQueHayDosLotesPendientes();

        cuandoDosWorkersReclamanAlaVez();

        entoncesCadaWorkerRecibeUnLoteDistinto();
        entoncesNoQuedanLotesPorReclamar();
    }

    @Test
    @DisplayName("Dos reservas concurrentes no superan el límite diario")
    void dosReservasConcurrentesNoSuperanElLimiteDiario() throws Exception {
        dadoDosLotesReclamados();

        cuandoSeReservaUnaPaginaPorLoteConLimiteDeUna();

        entoncesSoloUnaReservaTieneEfecto();
        entoncesQuedaUnaPaginaReservadaYNingunaCobrada();
    }

    @Test
    @DisplayName("No reserva por encima del límite aun sin historial")
    void noReservaPorEncimaDelLimiteAunSinHistorial() {
        dadoUnLoteReclamado();

        cuandoSeReservanDosPaginasConLimiteDeUna();

        entoncesLaReservaSeRechaza();
        entoncesNoQuedaNingunaPaginaReservada();
    }

    @Test
    @DisplayName("Un token ajeno no puede reservar consumo")
    void unTokenAjenoNoPuedeReservarConsumo() {
        dadoUnLoteReclamado();

        cuandoSeReservaConUnTokenAjeno();

        entoncesLaReservaSeRechaza();
    }

    @Test
    @DisplayName("Una reserva no se contabiliza dos veces")
    void unaReservaNoSeContabilizaDosVeces() {
        dadoUnLoteReclamado();
        dadoUnaPaginaReservada();

        cuandoSeRepiteLaReservaDeUnaPagina();

        entoncesLaReservaSeRechaza();
        entoncesQuedaUnaPaginaReservadaYNingunaCobrada();
    }

    @Test
    @DisplayName("Persiste la propuesta una sola vez aunque se repita la finalización")
    void persisteLaPropuestaUnaSolaVezAunqueSeRepitaLaFinalizacion() {
        dadoUnLoteReclamadoConUnaPaginaReservada();

        cuandoSeFinalizaConExitoDosVeces();

        entoncesElLoteQuedaListoConLaPropuestaDeUnIntento();
        entoncesHayUnaSolaLecturaDeIa();
    }

    @Test
    @DisplayName("Cobra el consumo una sola vez aunque se repita la finalización")
    void cobraElConsumoUnaSolaVezAunqueSeRepitaLaFinalizacion() {
        dadoUnLoteReclamadoConUnaPaginaReservada();

        cuandoSeFinalizaConExitoDosVeces();

        entoncesSeCobraUnaPaginaConSusTokensYSinReservas();
    }

    @Test
    @DisplayName("Un intento que cruza medianoche conserva el día de su reserva")
    void unIntentoQueCruzaMedianocheConservaElDiaDeSuReserva() {
        dadoUnLoteReclamadoConUnaPaginaReservada();

        cuandoSeFinalizaConExitoAlDiaSiguiente();

        entoncesElConsumoQuedaEnElDiaDeLaReserva();
    }

    @Test
    @DisplayName("Un reclamo sin finalizar aparece entre los lotes vencidos")
    void unReclamoSinFinalizarApareceEntreLosLotesVencidos() {
        dadoUnLoteReclamadoConUnaPaginaReservada();

        cuandoSeConsultanLosLotesVencidosDentroDeDosMinutos();

        entoncesElLoteReclamadoEstaVencido();
    }

    @Test
    @DisplayName("Un lote con reintento diferido no se reclama antes de su fecha")
    void unLoteConReintentoDiferidoNoSeReclamaAntesDeSuFecha() {
        dadoUnLoteReclamadoConUnaPaginaReservada();
        dadoQueElLoteSeDifiereTresMinutos();

        cuandoUnWorkerReclamaAlosDosMinutos();

        entoncesNoHayLotesParaReclamar();
    }

    @Test
    @DisplayName("Un reclamo vencido se recupera y se descarta el resultado tardío")
    void unReclamoVencidoSeRecuperaYSeDescartaElResultadoTardio() {
        dadoUnLoteRecuperadoPorOtroWorker();

        cuandoElWorkerViejoFinalizaConExito();

        entoncesElLoteSigueProcesandoseConElReclamoNuevo();
        entoncesElConsumoSigueIncompleto();
    }

    @Test
    @DisplayName("Los errores anteriores al proveedor no consumen páginas")
    void losErroresAnterioresAlProveedorNoConsumenPaginas() {
        dadoUnLoteReclamado();

        cuandoSeFinalizaConUnaFotoInvalida();

        entoncesElLoteQuedaEnErrorPorLaFotoInvalida();
        entoncesNoSeCobraNingunaPagina();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"modelo-real"})
    @DisplayName("Registra una falla con consumo desconocido y el modelo configurado")
    void registraUnaFallaConConsumoDesconocidoYElModeloConfigurado(String modelo) {
        dadoUnLoteReclamadoConUnaPaginaReservada();

        cuandoSeFinalizaConTimeoutConElModelo(modelo);

        entoncesSeCobraUnaPaginaConConsumoDesconocido();
        entoncesLaLecturaQuedaConElProveedorDePrueba();
    }

    @Test
    @DisplayName("Reencola un lote en error del negocio propio y reinicia el contador")
    void reencolaUnLoteEnErrorDelNegocioPropioYReiniciaElContador() {
        dadoUnLoteEnError();

        cuandoElNegocioPropioReencolaElLote();

        entoncesSeReencola();
        entoncesElContadorDeIntentosVuelveACero();
    }

    @Test
    @DisplayName("Un negocio ajeno no puede reencolar ni ver el lote")
    void unNegocioAjenoNoPuedeReencolarNiVerElLote() {
        dadoUnLoteEnError();

        cuandoUnNegocioAjenoReencolaElLote();

        entoncesNoSeReencola();
        entoncesUnNegocioAjenoNoVeElLote();
    }

    @Test
    @DisplayName("Un lote ya reencolado no se puede reencolar otra vez")
    void unLoteYaReencoladoNoSePuedeReencolarOtraVez() {
        dadoUnLoteEnError();
        dadoQueElNegocioPropioLoReencolo();

        cuandoElNegocioPropioReencolaElLote();

        entoncesNoSeReencola();
    }

    @Test
    @DisplayName("Carga las fotos ordenadas del lote y su bloque destino")
    void cargaLasFotosOrdenadasDelLoteYSuBloqueDestino() {
        dadoUnLoteConDosFotosDesordenadasYUnBloqueDestino();

        cuandoUnWorkerReclamaElLote();

        entoncesElLoteApuntaAlBloqueDestino();
        entoncesLasFotosSalenOrdenadasPorPosicion();
    }

    @Test
    @DisplayName("Carga el contexto tipado del negocio sin mezclar datos de otro negocio")
    void cargaElContextoTipadoDelNegocioSinMezclarDatosDeOtroNegocio() {
        dadoUnBloqueConUnCampoDeOpciones();

        cuandoSeCarganLosBloquesDelNegocio();

        entoncesSaleElBloqueConSuCampoYSusOpciones();
        entoncesOtroNegocioNoTieneBloques();
    }

    @Test
    @DisplayName("Lee a qué tabla apunta cada campo de conexión")
    void leeAQueTablaApuntaCadaCampoDeConexion() {
        dadoUnaTablaDeVentasConUnCampoDeConexionAClientes();

        cuandoSeLeenLosCamposDeLaTablaDeVentas();

        entoncesElCampoDeConexionApuntaAClientesYElOtroANinguna();
    }

    @Test
    @DisplayName("Las tablas del negocio salen con el nombre de su proyecto, sin las que sólo tienen fórmulas")
    void lasTablasDelNegocioSalenConElNombreDeSuProyectoSinLasQueSoloTienenFormulas() {
        dadoTresTablasDeInventarioDeLasQueSoloUnaTieneUnCampoCargable();

        cuandoSeListanLasTablasDelNegocio();

        entoncesSoloSaleLaTablaCargableConSuProyecto();
        entoncesOtroNegocioNoTieneTablas();
    }

    // --- dado ---

    private void dadoDosLotesConFotoYUnBloqueDeVentas() throws Exception {
        primerLote = insertarLote();
        segundoLote = insertarLote();
        bloqueId = insertarBloqueSimulado();
        insertarFoto(primerLote, 0, "primera.jpg");
        insertarFoto(segundoLote, 0, "segunda.jpg");
        escribirArchivos();
    }

    private void dadoQueHayDosLotesPendientes() {
        insertarLote();
        insertarLote();
    }

    private void dadoDosLotesReclamados() {
        insertarLote();
        insertarLote();
        trabajos = List.of(reclamarLote(), reclamarLote());
    }

    private void dadoUnLoteReclamado() {
        loteId = insertarLote();
        lote = reclamarLote();
    }

    private void dadoUnaPaginaReservada() {
        assertThat(reservar(lote, 1, 10)).isTrue();
    }

    private void dadoUnLoteReclamadoConUnaPaginaReservada() {
        dadoUnLoteReclamado();
        procesamientoRepository.reservar(lote, 1, 100, AHORA);
    }

    private void dadoQueElLoteSeDifiereTresMinutos() {
        procesamientoRepository.finalizar(lote, new ResultadoIntento(null, "Timeout", true),
                null, AHORA.plusMinutes(2), AHORA.plusMinutes(3));
    }

    private void dadoUnLoteRecuperadoPorOtroWorker() {
        dadoUnLoteReclamadoConUnaPaginaReservada();
        loteViejo = lote;
        dadoQueElLoteSeDifiereTresMinutos();
        loteNuevo = procesamientoRepository.reclamar(AHORA.plusMinutes(3), AHORA.plusMinutes(5)).orElseThrow();
    }

    private void dadoUnLoteEnError() {
        dadoUnLoteReclamado();
        procesamientoRepository.finalizar(lote, new ResultadoIntento(null, "Error", false), null, AHORA, null);
    }

    private void dadoQueElNegocioPropioLoReencolo() {
        assertThat(procesamientoRepository.reencolar(negocioId, loteId, AHORA)).isTrue();
    }

    private void dadoUnLoteConDosFotosDesordenadasYUnBloqueDestino() {
        loteId = insertarLote();
        dadoUnBloqueConUnCampoDeOpciones();
        insertarFoto(loteId, 1, "segunda.jpg");
        insertarFoto(loteId, 0, "primera.jpg");
        jdbcTemplate.update("UPDATE lote_subida SET bloque_destino_id = ? WHERE id = ?", bloqueId.valor(),
                loteId.valor());
    }

    private void dadoUnBloqueConUnCampoDeOpciones() {
        proyectoId = insertarProyecto("Proyecto");
        bloqueId = insertarBloque(proyectoId, "Ventas");
        campoId = CampoId.nuevo();
        jdbcTemplate.update("""
                INSERT INTO campo (id, negocio_id, bloque_id, nombre, tipo, opciones)
                VALUES (?, ?, ?, 'Producto', 'OPCION', '["A", "B"]')
                """, campoId.valor(), negocioId.valor(), bloqueId.valor());
    }

    private void dadoUnaTablaDeVentasConUnCampoDeConexionAClientes() {
        proyectoId = insertarProyecto("Proyecto");
        clientesId = insertarBloque(proyectoId, "Clientes");
        ventasId = insertarBloque(proyectoId, "Ventas");
        insertarCampoDeConexion(ventasId, clientesId);
        insertarCampo(ventasId, "Monto", "NUMERO");
    }

    private void dadoTresTablasDeInventarioDeLasQueSoloUnaTieneUnCampoCargable() {
        proyectoId = insertarProyecto("Mi panadería");
        ventasId = insertarTablaDeInventario();
        insertarCampo(ventasId, "Monto", "NUMERO");
        insertarCampoFormula(insertarTablaDeInventario(), "Total");
        insertarTablaDeInventario();
    }

    // --- cuando ---

    private void cuandoElWorkerProcesaDosVeces() {
        ProcesarLote procesarLote = elWorker();
        procesarLote.execute();
        procesarLote.execute();
    }

    private void cuandoDosWorkersReclamanAlaVez() throws Exception {
        reclamos = concurrentemente(() -> procesamientoRepository.reclamar(AHORA, AHORA.plusMinutes(2)));
    }

    private void cuandoSeReservaUnaPaginaPorLoteConLimiteDeUna() throws Exception {
        AtomicInteger indice = new AtomicInteger();
        reservas = concurrentemente(() ->
                procesamientoRepository.reservar(trabajos.get(indice.getAndIncrement()), 1, 1, AHORA));
    }

    private void cuandoSeReservanDosPaginasConLimiteDeUna() {
        resultado = reservar(lote, 2, 1);
    }

    private void cuandoSeReservaConUnTokenAjeno() {
        LoteTrabajo falso = new LoteTrabajo(lote.id(), negocioId, null, ReclamoId.nuevo(), 1);
        resultado = reservar(falso, 1, 10);
    }

    private void cuandoSeRepiteLaReservaDeUnaPagina() {
        resultado = reservar(lote, 1, 10);
    }

    private void cuandoSeFinalizaConExitoDosVeces() {
        ResultadoIntento exito = new ResultadoIntento(ProcesamientoTestData.propuesta(), null, false);
        procesamientoRepository.finalizar(lote, exito, BigDecimal.ZERO, AHORA, null);
        procesamientoRepository.finalizar(lote, exito, BigDecimal.ZERO, AHORA, null);
    }

    private void cuandoSeFinalizaConExitoAlDiaSiguiente() {
        procesamientoRepository.finalizar(lote, new ResultadoIntento(ProcesamientoTestData.propuesta(), null, false),
                BigDecimal.ZERO, AHORA.plusDays(1), null);
    }

    private void cuandoSeConsultanLosLotesVencidosDentroDeDosMinutos() {
        vencidos = procesamientoRepository.vencidos(AHORA.plusMinutes(2));
    }

    private void cuandoUnWorkerReclamaAlosDosMinutos() {
        reclamos = List.of(procesamientoRepository.reclamar(AHORA.plusMinutes(2), AHORA.plusMinutes(4)));
    }

    private void cuandoElWorkerViejoFinalizaConExito() {
        procesamientoRepository.finalizar(loteViejo,
                new ResultadoIntento(ProcesamientoTestData.propuesta(), null, false),
                BigDecimal.ZERO, AHORA.plusMinutes(3), null);
    }

    private void cuandoSeFinalizaConUnaFotoInvalida() {
        procesamientoRepository.finalizar(lote, new ResultadoIntento(null, "Foto inválida", false),
                null, AHORA, null);
    }

    private void cuandoSeFinalizaConTimeoutConElModelo(String modelo) {
        ProcesamientoRepositoryImpl repository = new ProcesamientoRepositoryImpl(namedParameterJdbcTemplate,
                objectMapper, new IaProperties("prueba", modelo, Duration.ofSeconds(1), 10, 0.8, null));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> repository.finalizar(
                lote, new ResultadoIntento(null, "Timeout", false), null, AHORA, null));
    }

    private void cuandoElNegocioPropioReencolaElLote() {
        resultado = procesamientoRepository.reencolar(negocioId, loteId, AHORA);
    }

    private void cuandoUnNegocioAjenoReencolaElLote() {
        resultado = procesamientoRepository.reencolar(NegocioId.nuevo(), loteId, AHORA);
    }

    private void cuandoUnWorkerReclamaElLote() {
        lote = reclamarLote();
    }

    private void cuandoSeCarganLosBloquesDelNegocio() {
        bloques = procesamientoRepository.bloques(negocioId);
    }

    private void cuandoSeLeenLosCamposDeLaTablaDeVentas() {
        camposDeVentas = procesamientoRepository.bloques(negocioId).stream()
                .filter(bloque -> bloque.id().equals(ventasId))
                .findFirst().orElseThrow().campos();
    }

    private void cuandoSeListanLasTablasDelNegocio() {
        tablas = procesamientoRepository.tablas(negocioId);
    }

    // --- entonces ---

    private void entoncesAmbosLotesQuedanListosSobreElBloqueDeVentas() {
        EstadoLectura primera = estadoDe(primerLote);
        EstadoLectura segunda = estadoDe(segundoLote);
        assertThat(primera.estado()).isEqualTo(EstadoLote.LISTO);
        assertThat(segunda.estado()).isEqualTo(EstadoLote.LISTO);
        assertThat(primera.propuesta().lectura().bloque().bloqueExistenteId()).isEqualTo(bloqueId);
        assertThat(segunda.propuesta().lectura().bloque().bloqueExistenteId()).isEqualTo(bloqueId);
        assertThat(primera.propuesta().mapeoCampos()).hasSize(6);
        assertThat(segunda.propuesta().mapeoCampos()).isEqualTo(primera.propuesta().mapeoCampos());
    }

    private void entoncesSeCobraUnaPaginaPorLote() {
        assertThat(consumoDelDia().paginas()).isEqualTo(2);
    }

    private void entoncesNoSeCreaNingunRegistro() {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM registro WHERE negocio_id = ?",
                Integer.class, negocioId.valor())).isZero();
    }

    private void entoncesCadaWorkerRecibeUnLoteDistinto() {
        assertThat(reclamos).allMatch(Optional::isPresent);
        assertThat(reclamos.getFirst().orElseThrow().id()).isNotEqualTo(reclamos.getLast().orElseThrow().id());
    }

    private void entoncesNoQuedanLotesPorReclamar() {
        assertThat(procesamientoRepository.reclamar(AHORA, AHORA.plusMinutes(2))).isEmpty();
    }

    private void entoncesSoloUnaReservaTieneEfecto() {
        assertThat(reservas).containsExactlyInAnyOrder(true, false);
    }

    private void entoncesQuedaUnaPaginaReservadaYNingunaCobrada() {
        assertThat(consumoDelDia().reservadas()).isEqualTo(1);
        assertThat(consumoDelDia().paginas()).isZero();
    }

    private void entoncesLaReservaSeRechaza() {
        assertThat(resultado).isFalse();
    }

    private void entoncesNoQuedaNingunaPaginaReservada() {
        assertThat(consumoDelDia().reservadas()).isZero();
    }

    private void entoncesElLoteQuedaListoConLaPropuestaDeUnIntento() {
        EstadoLectura estado = estadoDe(loteId);
        assertThat(estado.estado()).isEqualTo(EstadoLote.LISTO);
        assertThat(estado.intentos()).isEqualTo(1);
        assertThat(estado.propuesta().mapeoCampos()).containsEntry("monto", ProcesamientoTestData.CAMPO_ID);
        assertThat(estado.propuesta().lectura().filas()).hasSize(1);
    }

    private void entoncesHayUnaSolaLecturaDeIa() {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM lectura_ia WHERE lote_id = ?",
                Integer.class, loteId.valor())).isEqualTo(1);
    }

    private void entoncesSeCobraUnaPaginaConSusTokensYSinReservas() {
        ConsumoDiario consumo = consumoDelDia();
        assertThat(consumo.paginas()).isEqualTo(1);
        assertThat(consumo.reservadas()).isZero();
        assertThat(consumo.tokensEntrada()).isEqualTo(10);
        assertThat(consumo.tokensSalida()).isEqualTo(20);
        assertThat(consumo.costoEstimadoUsd()).isEqualByComparingTo("0");
        assertThat(consumo.consumoCompleto()).isTrue();
    }

    private void entoncesElConsumoQuedaEnElDiaDeLaReserva() {
        assertThat(consumoDelDia().tokensEntrada()).isEqualTo(10);
        assertThat(procesamientoRepository.consumo(negocioId, AHORA.plusDays(1).toLocalDate()).paginas()).isZero();
    }

    private void entoncesElLoteReclamadoEstaVencido() {
        assertThat(vencidos).contains(lote);
    }

    private void entoncesNoHayLotesParaReclamar() {
        assertThat(reclamos).containsExactly(Optional.empty());
    }

    private void entoncesElLoteSigueProcesandoseConElReclamoNuevo() {
        assertThat(estadoDe(loteId).estado()).isEqualTo(EstadoLote.PROCESANDO);
        assertThat(loteNuevo.reclamoId()).isNotEqualTo(loteViejo.reclamoId());
        assertThat(loteNuevo.intentos()).isEqualTo(2);
    }

    private void entoncesElConsumoSigueIncompleto() {
        assertThat(consumoDelDia().consumoCompleto()).isFalse();
    }

    private void entoncesElLoteQuedaEnErrorPorLaFotoInvalida() {
        EstadoLectura estado = estadoDe(loteId);
        assertThat(estado.estado()).isEqualTo(EstadoLote.ERROR);
        assertThat(estado.motivoError()).isEqualTo("Foto inválida");
        assertThat(estado.propuesta()).isNull();
    }

    private void entoncesNoSeCobraNingunaPagina() {
        ConsumoDiario consumo = consumoDelDia();
        assertThat(consumo.paginas()).isZero();
        assertThat(consumo.consumoCompleto()).isTrue();
        assertThat(consumo.costoEstimadoUsd()).isEqualByComparingTo("0");
    }

    private void entoncesSeCobraUnaPaginaConConsumoDesconocido() {
        ConsumoDiario consumo = consumoDelDia();
        assertThat(consumo.paginas()).isEqualTo(1);
        assertThat(consumo.costoEstimadoUsd()).isNull();
        assertThat(consumo.consumoCompleto()).isFalse();
    }

    private void entoncesLaLecturaQuedaConElProveedorDePrueba() {
        assertThat(jdbcTemplate.queryForObject("SELECT proveedor FROM lectura_ia WHERE lote_id = ?",
                String.class, lote.id().valor())).isEqualTo("prueba");
    }

    private void entoncesSeReencola() {
        assertThat(resultado).isTrue();
    }

    private void entoncesNoSeReencola() {
        assertThat(resultado).isFalse();
    }

    private void entoncesElContadorDeIntentosVuelveACero() {
        assertThat(estadoDe(loteId).intentos()).isZero();
    }

    private void entoncesUnNegocioAjenoNoVeElLote() {
        assertThat(procesamientoRepository.obtener(NegocioId.nuevo(), loteId)).isEmpty();
    }

    private void entoncesElLoteApuntaAlBloqueDestino() {
        assertThat(lote.bloqueDestinoId()).isEqualTo(bloqueId);
    }

    private void entoncesLasFotosSalenOrdenadasPorPosicion() {
        assertThat(procesamientoRepository.fotos(lote)).extracting("clave")
                .containsExactly(negocioId + "/primera.jpg", negocioId + "/segunda.jpg");
    }

    private void entoncesSaleElBloqueConSuCampoYSusOpciones() {
        assertThat(bloques).hasSize(1);
        assertThat(bloques.getFirst().campos().getFirst().id()).isEqualTo(campoId);
        assertThat(bloques.getFirst().campos().getFirst().opciones()).containsExactly("A", "B");
    }

    private void entoncesOtroNegocioNoTieneBloques() {
        assertThat(procesamientoRepository.bloques(NegocioId.nuevo())).isEmpty();
    }

    private void entoncesElCampoDeConexionApuntaAClientesYElOtroANinguna() {
        assertThat(camposDeVentas).extracting(CampoConocido::nombre, CampoConocido::bloqueRelacionadoId)
                .containsExactlyInAnyOrder(Tuple.tuple("Cliente", clientesId), Tuple.tuple("Monto", null));
    }

    private void entoncesSoloSaleLaTablaCargableConSuProyecto() {
        assertThat(tablas).hasSize(1);
        assertThat(tablas.getFirst().bloque().id()).isEqualTo(ventasId);
        assertThat(tablas.getFirst().proyecto()).isEqualTo("Mi panadería");
        assertThat(tablas.getFirst().tipo()).isEqualTo(TipoBloque.INVENTARIO);
    }

    private void entoncesOtroNegocioNoTieneTablas() {
        assertThat(procesamientoRepository.tablas(NegocioId.nuevo())).isEmpty();
    }

    // --- datos ---

    private ProcesarLote elWorker() {
        return new ProcesarLote(procesamientoRepository, new ArchivoCuadernoLocalAdapter(archivos, 10),
                lectorCuadernoPort, null, null, controlPlanService,
                ProcesamientoTestData.politica(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
    }

    private LoteTrabajo reclamarLote() {
        return procesamientoRepository.reclamar(AHORA, AHORA.plusMinutes(2)).orElseThrow();
    }

    private boolean reservar(LoteTrabajo loteAReservar, int paginas, int limiteDiario) {
        return procesamientoRepository.reservar(loteAReservar, paginas, limiteDiario, AHORA);
    }

    private ConsumoDiario consumoDelDia() {
        return procesamientoRepository.consumo(negocioId, AHORA.toLocalDate());
    }

    private EstadoLectura estadoDe(LoteId id) {
        return procesamientoRepository.obtener(negocioId, id).orElseThrow();
    }

    private LoteId insertarLote() {
        LoteId id = LoteId.nuevo();
        jdbcTemplate.update("INSERT INTO lote_subida (id, negocio_id) VALUES (?, ?)", id.valor(), negocioId.valor());
        return id;
    }

    private void insertarFoto(LoteId lotePropio, int posicion, String clave) {
        jdbcTemplate.update("""
                INSERT INTO foto_cuaderno (negocio_id, lote_id, clave_almacenamiento, tipo_contenido,
                    tamanio_bytes, posicion) VALUES (?, ?, ?, 'image/jpeg', 3, ?)
                """, negocioId.valor(), lotePropio.valor(), negocioId.valor() + "/" + clave, posicion);
    }

    private void escribirArchivos() throws Exception {
        Files.createDirectory(archivos.resolve(negocioId.toString()));
        Files.write(archivos.resolve(negocioId + "/primera.jpg"), new byte[]{1, 2, 3});
        Files.write(archivos.resolve(negocioId + "/segunda.jpg"), new byte[]{4, 5, 6});
    }

    private ProyectoId insertarProyecto(String nombre) {
        ProyectoId id = ProyectoId.nuevo();
        jdbcTemplate.update("INSERT INTO proyecto (id, negocio_id, nombre) VALUES (?, ?, ?)",
                id.valor(), negocioId.valor(), nombre);
        return id;
    }

    private BloqueId insertarBloque(ProyectoId proyecto, String nombre) {
        BloqueId id = BloqueId.nuevo();
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, ?)",
                id.valor(), negocioId.valor(), proyecto.valor(), nombre);
        return id;
    }

    private BloqueId insertarTablaDeInventario() {
        BloqueId id = BloqueId.nuevo();
        jdbcTemplate.update("""
                INSERT INTO bloque (id, negocio_id, proyecto_id, nombre, tipo) VALUES (?, ?, ?, ?, 'INVENTARIO')
                """, id.valor(), negocioId.valor(), proyectoId.valor(), "Tabla " + id.valor().toString().substring(0, 4));
        return id;
    }

    private void insertarCampo(BloqueId bloque, String nombre, String tipo) {
        jdbcTemplate.update("""
                INSERT INTO campo (negocio_id, bloque_id, nombre, tipo, opciones) VALUES (?, ?, ?, ?, '[]')
                """, negocioId.valor(), bloque.valor(), nombre, tipo);
    }

    private void insertarCampoFormula(BloqueId bloque, String nombre) {
        jdbcTemplate.update("""
                INSERT INTO campo (negocio_id, bloque_id, nombre, tipo, opciones, formula)
                VALUES (?, ?, ?, 'FORMULA', '[]', '{}')
                """, negocioId.valor(), bloque.valor(), nombre);
    }

    private void insertarCampoDeConexion(BloqueId bloque, BloqueId relacionado) {
        jdbcTemplate.update("""
                INSERT INTO campo (negocio_id, bloque_id, nombre, tipo, opciones, bloque_relacionado_id)
                VALUES (?, ?, 'Cliente', 'RELACION', '[]', ?)
                """, negocioId.valor(), bloque.valor(), relacionado.valor());
    }

    private BloqueId insertarBloqueSimulado() throws Exception {
        ProyectoId proyecto = insertarProyecto("Proyecto");
        BloqueId bloque = insertarBloque(proyecto, "Ventas");
        try (var stream = new ClassPathResource("ia/lectura-simulada.json").getInputStream()) {
            for (var campo : objectMapper.readTree(stream).path("bloque").path("campos")) {
                insertarCampoSimulado(bloque, campo);
            }
        }
        return bloque;
    }

    private void insertarCampoSimulado(BloqueId bloque, JsonNode campo) {
        jdbcTemplate.update("""
                INSERT INTO campo (negocio_id, bloque_id, nombre, tipo, opciones) VALUES (?, ?, ?, ?, ?::jsonb)
                """, negocioId.valor(), bloque.valor(), campo.path("nombre").asString(),
                campo.path("tipo").asString(), objectMapper.writeValueAsString(campo.path("opciones")));
    }

    private <T> List<T> concurrentemente(Callable<T> operacion) throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch barrera = new CountDownLatch(2);
            Callable<T> sincronizada = () -> {
                barrera.countDown();
                assertThat(barrera.await(5, TimeUnit.SECONDS)).isTrue();
                return operacion.call();
            };
            var primero = executor.submit(sincronizada);
            var segundo = executor.submit(sincronizada);
            return List.of(primero.get(10, TimeUnit.SECONDS), segundo.get(10, TimeUnit.SECONDS));
        }
    }
}
