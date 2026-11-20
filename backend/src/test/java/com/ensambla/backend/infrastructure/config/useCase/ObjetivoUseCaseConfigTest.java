package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.CandidatosDelNegocio;
import com.ensambla.backend.domain.model.objetivo.ConsultaObjetivoIa;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.DecisionPaso;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.OrigenPropuesta;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;
import com.ensambla.backend.domain.model.plan.CuotaObjetivosIa;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.ConversacionObjetivosIaPort;
import com.ensambla.backend.domain.port.RenovacionManualesIaPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ProgresoObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.RespuestaMiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ResumenSemanalRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.objetivo.MonitoreoObjetivosService;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.abandonarMiniObjetivo.AbandonarMiniObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.guardarObjetivoConSami.GuardarObjetivoConSamiRequest;
import com.ensambla.backend.domain.useCase.objetivo.guardarObjetivoConSami.GuardarObjetivoConSamiRequest.MiniElegido;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisionesRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisionesResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivosRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivosResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes.ListarRespuestasPendientesRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes.ListarRespuestasPendientesResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse;
import com.ensambla.backend.domain.useCase.objetivo.medirObjetivos.MedirObjetivosRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSami;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSamiRequest;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSamiResponse;
import com.ensambla.backend.domain.useCase.objetivo.registrarDecisionPaso.RegistrarDecisionPasoRequest;
import com.ensambla.backend.domain.useCase.objetivo.reemplazarMiniObjetivo.ReemplazarMiniObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo.ResponderMiniObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo.ResponderMiniObjetivoResponse;
import com.ensambla.backend.infrastructure.ia.ObjetivosIaProperties;
import com.ensambla.backend.infrastructure.objetivos.ObjetivosCierreProperties;
import com.ensambla.backend.infrastructure.objetivos.ObjetivosMonitoreoProperties;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.JUSTIFICACION_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.TITULO_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.manual;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.mensajes;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.respuestaIa;
import static com.ensambla.backend.support.DecisionTestData.aviso;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO_SIN_META;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjetivoUseCaseConfigTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final Clock CLOCK = Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC);
    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);
    private static final int LIMITE_DIARIO_CONFIGURADO = 7;

    private final ObjetivoUseCaseConfig config = new ObjetivoUseCaseConfig();
    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final SistemaRepository sistemaRepository = mock(SistemaRepository.class);
    private final ProgresoObjetivoRepository progresoObjetivoRepository = mock(ProgresoObjetivoRepository.class);
    private final DecisionObjetivoRepository decisionObjetivoRepository = mock(DecisionObjetivoRepository.class);
    private final ResumenSemanalRepository resumenSemanalRepository = mock(ResumenSemanalRepository.class);
    private final RespuestaMiniObjetivoRepository respuestaMiniObjetivoRepository =
            mock(RespuestaMiniObjetivoRepository.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final ConsumoSamiRepository consumoSamiRepository = mock(ConsumoSamiRepository.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private final ConversacionObjetivosIaPort conversacionObjetivosIaPort = mock(ConversacionObjetivosIaPort.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final AgregadoZonaRepository agregadoZonaRepository = mock(AgregadoZonaRepository.class);

    private CandidatosDelNegocio candidatosDelNegocio;
    private ListarMiniObjetivosResponse minis;
    private ListarCandidatosResponse candidatos;
    private ObtenerPasosResponse pasos;
    private ObtenerProgresoResponse progreso;
    private ListarDecisionesResponse decisiones;
    private ListarSemanasResponse semanas;
    private ListarRespuestasPendientesResponse pendientes;
    private ResponderMiniObjetivoResponse respondida;
    private ProponerObjetivoConSamiResponse respuestaDeSami;
    private ObjetivoItem objetivoGuardado;
    private Throwable error;

    @Test
    @DisplayName("El wiring de la propuesta carga los sistemas del negocio y no encuentra candidatos sin datos")
    void elWiringDeLaPropuestaCargaLosSistemasDelNegocioYNoEncuentraCandidatosSin() {
        dadoUnNegocioSinSistemasNiZona();

        cuandoSePideLaPropuestaDeCandidatos();

        entoncesNoHayCandidatosSinDatos();
    }

    @Test
    @DisplayName("El wiring de listar mini-objetivos conecta los repositorios")
    void elWiringDeListarMiniObjetivosConectaLosRepositorios() {
        dadoObjetivoYMiniObjetivo();
        dadoLosMinisDelObjetivo();

        cuandoSeListanLosMiniObjetivos();

        entoncesSeListaUnMiniObjetivo();
    }

    @Test
    @DisplayName("El wiring de listar candidatos conecta la propuesta y el reloj")
    void elWiringDeListarCandidatosConectaLaPropuestaYElReloj() {
        dadoObjetivoYMiniObjetivo();
        dadoLosMinisDelObjetivo();
        dadoUnNegocioSinSistemasNiZona();

        cuandoSeListanLosCandidatos();

        entoncesNoHayCandidatosParaElegir();
    }

    @Test
    @DisplayName("El wiring de reemplazar conecta la propuesta: sin candidatos el elegido no está disponible")
    void elWiringDeReemplazarConectaLaPropuestaSinCandidatosElElegidoNoEsta() {
        dadoObjetivoYMiniObjetivo();
        dadoLosMinisDelObjetivo();
        dadoUnNegocioSinSistemasNiZona();

        cuandoSeReemplazaUnMiniObjetivo();

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("El wiring de abandonar conecta los repositorios y guarda el mini-objetivo quitado")
    void elWiringDeAbandonarConectaLosRepositoriosYGuardaElMiniObjetivoQuitado() {
        dadoObjetivoYMiniObjetivo();

        cuandoSeAbandonaUnMiniObjetivo();

        entoncesSeGuardaElMiniObjetivoAbandonado();
    }

    @Test
    @DisplayName("El wiring de pasos conecta las fuentes reales del negocio")
    void elWiringDePasosConectaLasFuentesRealesDelNegocio() {
        dadoUnNegocioSinSistemasNiZona();
        dadoElNegocioDelUsuario();

        cuandoSeObtienenLosPasos();

        entoncesNoHayPasos();
    }

    @Test
    @DisplayName("El wiring del monitoreo mide cada objetivo y guarda su foto del día")
    void elWiringDelMonitoreoMideCadaObjetivoYGuardaSuFotoDelDia() {
        dadoUnNegocioConUnObjetivoSinSistemas();

        cuandoSeMonitoreanLosObjetivos();

        entoncesSeGuardaLaFotoDelObjetivo();
    }

    @Test
    @DisplayName("El wiring de medir mide el negocio de la sesión y guarda su foto del día")
    void elWiringDeMedirMideElNegocioDeLaSesionYGuardaSuFotoDelDia() {
        dadoUnNegocioConUnObjetivoSinSistemas();
        dadoElNegocioDelUsuario();

        cuandoSeMidenLosObjetivos();

        entoncesSeGuardaUnaFoto();
    }

    @Test
    @DisplayName("El wiring del progreso busca el objetivo del negocio de la sesión")
    void elWiringDelProgresoBuscaElObjetivoDelNegocioDeLaSesion() {
        dadoObjetivoYMiniObjetivo();
        dadoUnNegocioSinSistemasNiZona();

        cuandoSeObtieneElProgreso();

        entoncesElProgresoTieneLaMetaYNoTieneSemanas();
    }

    @Test
    @DisplayName("El wiring del servicio de decisiones anota una decisión por objetivo activo")
    void elWiringDelServicioDeDecisionesAnotaUnaDecisionPorObjetivoActivo() {
        dadoUnObjetivoActivoConUnMiniObjetivo();

        cuandoSeRegistraUnAvisoAplicado();

        entoncesSeAnotaLaDecisionDelMiniObjetivo();
    }

    @Test
    @DisplayName("El wiring de decidir un paso busca el mini-objetivo del negocio de la sesión y guarda la decisión")
    void elWiringDeDecidirUnPasoBuscaElMiniObjetivoDelNegocioDeLaSesionYGuardaLa() {
        dadoObjetivoYMiniObjetivo();
        dadoQueSeGuardaLaDecision();

        cuandoSeRegistraLaDecisionDeUnPaso();

        entoncesSeGuardaLaDecisionDelPasoAceptado();
    }

    @Test
    @DisplayName("El wiring de listar decisiones busca el objetivo del negocio de la sesión")
    void elWiringDeListarDecisionesBuscaElObjetivoDelNegocioDeLaSesion() {
        dadoObjetivoYMiniObjetivo();

        cuandoSeListanLasDecisiones();

        entoncesNoHayDecisionesYSeBuscaronLasDelObjetivo();
    }

    @Test
    @DisplayName("El wiring del cierre semanal cierra la semana pendiente y guarda su resumen")
    void elWiringDelCierreSemanalCierraLaSemanaPendienteYGuardaSuResumen() {
        dadoUnObjetivoDeLaSemanaPasadaSinSistemas();

        cuandoSeCierranLasSemanas();

        entoncesSeGuardaElResumenDeLaSemana();
    }

    @Test
    @DisplayName("El wiring de listar semanas busca el objetivo del negocio de la sesión")
    void elWiringDeListarSemanasBuscaElObjetivoDelNegocioDeLaSesion() {
        dadoObjetivoYMiniObjetivo();

        cuandoSeListanLasSemanas();

        entoncesNoHaySemanasYSeBuscaronLasDelObjetivo();
    }

    @Test
    @DisplayName("El wiring de responder un mini-objetivo conecta los repositorios, la transacción y el reloj")
    void elWiringDeResponderUnMiniObjetivoConectaLosRepositoriosLaTransaccionYEl() {
        dadoUnManualSinResponder();

        cuandoSeRespondeElManual();

        entoncesSeGuardaLaRespuestaYLaDecision();
    }

    @Test
    @DisplayName("El wiring de listar respuestas pendientes conecta los repositorios")
    void elWiringDeListarRespuestasPendientesConectaLosRepositorios() {
        dadoUnManualPendiente();

        cuandoSeListanLasRespuestasPendientes();

        entoncesHayUnaRespuestaPendiente();
    }

    @Test
    @DisplayName("El wiring de proponer con Sami conecta la propuesta, la IA, el consumo y sus límites")
    void elWiringDeProponerConSamiConectaLaPropuestaLaIaElConsumoYSusLimites() {
        dadoUnaIaQueProponeUnaAccion();

        cuandoSePropone();

        entoncesSeProponeConLaIaYSeReservaConElLimiteConfigurado();
    }

    @Test
    @DisplayName("El wiring de proponer con Sami respeta si la conversación está apagada y el tope de mensajes")
    void elWiringDeProponerConSamiRespetaSiLaConversacionEstaApagadaYElTopeDe() {
        dadoUnNegocioConCuotaYSinSistemas();

        cuandoSeProponeConLaConversacionApagada();

        entoncesSeProponeElRespaldoSinMensajesRestantes();
    }

    @Test
    @DisplayName("El wiring de guardar con Sami conecta los repositorios, la propuesta, la transacción y el reloj")
    void elWiringDeGuardarConSamiConectaLosRepositoriosLaPropuestaLaTransaccionYEl() {
        dadoUnNegocioQueGuardaObjetivos();

        cuandoSeGuardaConSeguimientoManual();

        entoncesSeGuardaElObjetivoManualYSuMini();
    }

    @Test
    @DisplayName("El wiring de guardar con Sami conecta el control del plan y el consumo de la cuota")
    void elWiringDeGuardarConSamiConectaElControlDelPlanYElConsumoDeLaCuota() {
        dadoUnNegocioQueGuardaObjetivos();

        cuandoSeGuardaConUnaAccionDeSami();

        entoncesSeValidaYSeGastaLaCuota();
    }

    // --- arrange ---
    private void dadoElNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private void dadoUnNegocioSinSistemasNiZona() {
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoObjetivoYMiniObjetivo() {
        dadoElNegocioDelUsuario();
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID)).thenReturn(miniObjetivo());
    }

    private void dadoLosMinisDelObjetivo() {
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(miniObjetivo()));
    }

    private void dadoUnNegocioConUnObjetivoSinSistemas() {
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 50)).thenReturn(List.of(NEGOCIO_ID));
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivo()));
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
    }

    private void dadoUnObjetivoActivoConUnMiniObjetivo() {
        Objetivo delNegocio = objetivo();
        delNegocio.setNegocioId(NEGOCIO_ID);
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(delNegocio));
        dadoLosMinisDelObjetivo();
    }

    private void dadoQueSeGuardaLaDecision() {
        when(decisionObjetivoRepository.save(any(DecisionObjetivo.class))).thenAnswer(call -> call.getArgument(0));
    }

    private void dadoUnObjetivoDeLaSemanaPasadaSinSistemas() {
        Objetivo deLaSemanaPasada = objetivo();
        deLaSemanaPasada.setCreadoEn(AHORA.minusWeeks(1));
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 50)).thenReturn(List.of(NEGOCIO_ID));
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(deLaSemanaPasada));
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
    }

    private void dadoUnManualSinResponder() {
        dadoElNegocioDelUsuario();
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, OTRO_MINI_ID)).thenReturn(miniManual());
        when(respuestaMiniObjetivoRepository.save(any(RespuestaMiniObjetivo.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    private void dadoUnManualPendiente() {
        dadoElNegocioDelUsuario();
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivo()));
        when(miniObjetivoRepository.findAllPendientesDeRespuestaByNegocioId(NEGOCIO_ID))
                .thenReturn(List.of(miniManual()));
    }

    private void dadoUnaIaQueProponeUnaAccion() {
        dadoUnNegocioConCuotaYSinSistemas();
        when(consumoSamiRepository.reservar(any(), any(), anyInt(), any())).thenReturn(true);
        when(conversacionObjetivosIaPort.conversar(any(ConsultaObjetivoIa.class)))
                .thenReturn(respuestaIa(manual(TITULO_ACCION, JUSTIFICACION_ACCION)));
    }

    private void dadoUnNegocioConCuotaYSinSistemas() {
        dadoElNegocioDelUsuario();
        dadoUnNegocioSinSistemasNiZona();
        when(controlPlanService.cuotaObjetivosIa(eq(NEGOCIO_ID), any())).thenReturn(new CuotaObjetivosIa(0, 5));
    }

    private void dadoUnNegocioQueGuardaObjetivos() {
        dadoElNegocioDelUsuario();
        when(objetivoRepository.save(any(Objetivo.class))).thenAnswer(call -> {
            Objetivo objetivo = call.getArgument(0);
            objetivo.setId(OBJETIVO_ID);
            return objetivo;
        });
    }

    // --- act ---
    private void cuandoSePideLaPropuestaDeCandidatos() {
        candidatosDelNegocio = propuesta().candidatos(NEGOCIO_ID, HOY);
    }

    private void cuandoSeListanLosMiniObjetivos() {
        minis = config.listarMiniObjetivos(negocioRepository, objetivoRepository, miniObjetivoRepository)
                .execute(new ListarMiniObjetivosRequest(USUARIO_ID, OBJETIVO_ID));
    }

    private void cuandoSeListanLosCandidatos() {
        candidatos = config
                .listarCandidatos(negocioRepository, objetivoRepository, miniObjetivoRepository, propuesta(), CLOCK)
                .execute(new ListarCandidatosRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID));
    }

    private void cuandoSeReemplazaUnMiniObjetivo() {
        var reemplazar = config.reemplazarMiniObjetivo(
                negocioRepository, objetivoRepository, miniObjetivoRepository, propuesta(),
                new TransaccionDirecta(), CLOCK);
        error = catchThrowable(() -> reemplazar.execute(new ReemplazarMiniObjetivoRequest(
                USUARIO_ID, OBJETIVO_ID, MINI_ID, TipoMiniObjetivo.MEJORAR_MARGEN, null, null)));
    }

    private void cuandoSeAbandonaUnMiniObjetivo() {
        config.abandonarMiniObjetivo(
                        negocioRepository, objetivoRepository, miniObjetivoRepository, new TransaccionDirecta(), CLOCK)
                .execute(new AbandonarMiniObjetivoRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID));
    }

    private void cuandoSeObtienenLosPasos() {
        pasos = config.obtenerPasos(
                        negocioRepository,
                        sistemaRepository,
                        datosDeProyectoService,
                        zonaRepository,
                        agregadoZonaRepository,
                        metricas(),
                        Clock.systemUTC())
                .execute(new ObtenerPasosRequest(USUARIO_ID));
    }

    private void cuandoSeMonitoreanLosObjetivos() {
        config.monitorearObjetivos(
                        objetivoRepository, monitoreo(), new TransaccionDirecta(), CLOCK,
                        new ObjetivosMonitoreoProperties(50))
                .execute();
    }

    private void cuandoSeMidenLosObjetivos() {
        config.medirObjetivos(negocioRepository, monitoreo(), new TransaccionDirecta(), CLOCK)
                .execute(new MedirObjetivosRequest(USUARIO_ID));
    }

    private void cuandoSeObtieneElProgreso() {
        progreso = config.obtenerProgreso(
                        negocioRepository, objetivoRepository, miniObjetivoRepository, progresoObjetivoRepository,
                        propuesta(), resumenSemanalRepository, CLOCK)
                .execute(new ObtenerProgresoRequest(USUARIO_ID, OBJETIVO_ID));
    }

    private void cuandoSeRegistraUnAvisoAplicado() {
        Aviso aplicado = aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO);
        aplicado.setNegocioId(NEGOCIO_ID);
        config.decisionesObjetivoService(objetivoRepository, miniObjetivoRepository, decisionObjetivoRepository)
                .registrarAviso(aplicado, TipoDecision.AVISO_APLICADO, AHORA);
    }

    private void cuandoSeRegistraLaDecisionDeUnPaso() {
        config.registrarDecisionPaso(
                        negocioRepository, objetivoRepository, miniObjetivoRepository, decisionObjetivoRepository,
                        new TransaccionDirecta(), CLOCK)
                .execute(new RegistrarDecisionPasoRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID, DecisionPaso.ACEPTADO));
    }

    private void cuandoSeListanLasDecisiones() {
        decisiones = config.listarDecisiones(
                        negocioRepository, objetivoRepository, decisionObjetivoRepository, avisoRepository)
                .execute(new ListarDecisionesRequest(USUARIO_ID, OBJETIVO_ID));
    }

    private void cuandoSeCierranLasSemanas() {
        config.cerrarSemanasObjetivos(
                        objetivoRepository, miniObjetivoRepository, decisionObjetivoRepository,
                        avisoRepository, resumenSemanalRepository, propuesta(),
                        respuestaMiniObjetivoRepository, mock(RenovacionManualesIaPort.class),
                        controlPlanService, consumoSamiRepository, new TransaccionDirecta(),
                        CLOCK, new ObjetivosCierreProperties(8, 50), new ObjetivosIaProperties(false, 10, true, 6))
                .execute();
    }

    private void cuandoSeListanLasSemanas() {
        semanas = config.listarSemanas(negocioRepository, objetivoRepository, resumenSemanalRepository)
                .execute(new ListarSemanasRequest(USUARIO_ID, OBJETIVO_ID));
    }

    private void cuandoSeRespondeElManual() {
        respondida = config
                .responderMiniObjetivo(negocioRepository, objetivoRepository, miniObjetivoRepository,
                        respuestaMiniObjetivoRepository, decisionObjetivoRepository, new TransaccionDirecta(), CLOCK)
                .execute(new ResponderMiniObjetivoRequest(
                        USUARIO_ID, OBJETIVO_ID, OTRO_MINI_ID, HizoManual.SI, QuePaso.FUNCIONO, null));
    }

    private void cuandoSeListanLasRespuestasPendientes() {
        pendientes = config
                .listarRespuestasPendientes(negocioRepository, objetivoRepository, miniObjetivoRepository)
                .execute(new ListarRespuestasPendientesRequest(USUARIO_ID));
    }

    private void cuandoSePropone() {
        respuestaDeSami = config
                .proponerObjetivoConSami(negocioRepository, propuesta(), controlPlanService, consumoSamiRepository,
                        conversacionObjetivosIaPort, CLOCK,
                        new ObjetivosIaProperties(false, LIMITE_DIARIO_CONFIGURADO, true, 6))
                .execute(new ProponerObjetivoConSamiRequest(USUARIO_ID, mensajes()));
    }

    private void cuandoSeProponeConLaConversacionApagada() {
        ProponerObjetivoConSami apagada = config.proponerObjetivoConSami(negocioRepository, propuesta(),
                controlPlanService, consumoSamiRepository, conversacionObjetivosIaPort, CLOCK,
                new ObjetivosIaProperties(true, 10, false, 1));
        respuestaDeSami = apagada.execute(new ProponerObjetivoConSamiRequest(USUARIO_ID, mensajes()));
    }

    private void cuandoSeGuardaConSeguimientoManual() {
        objetivoGuardado = config
                .guardarObjetivoConSami(negocioRepository, objetivoRepository, miniObjetivoRepository, propuesta(),
                        controlPlanService, usoPlanRepository, new TransaccionDirecta(), CLOCK)
                .execute(new GuardarObjetivoConSamiRequest(USUARIO_ID, TEXTO_SIN_META, true, List.of()));
    }

    private void cuandoSeGuardaConUnaAccionDeSami() {
        MiniElegido accion = new MiniElegido(
                ModoMiniObjetivo.MANUAL, null, null, null, TITULO_ACCION, JUSTIFICACION_ACCION);
        config.guardarObjetivoConSami(negocioRepository, objetivoRepository, miniObjetivoRepository, propuesta(),
                        controlPlanService, usoPlanRepository, new TransaccionDirecta(), CLOCK)
                .execute(new GuardarObjetivoConSamiRequest(USUARIO_ID, TEXTO_SIN_META, false, List.of(accion)));
    }

    // --- assert ---
    private void entoncesNoHayCandidatosSinDatos() {
        assertThat(candidatosDelNegocio.candidatos()).isEmpty();
    }

    private void entoncesSeListaUnMiniObjetivo() {
        assertThat(minis.minis()).hasSize(1);
    }

    private void entoncesNoHayCandidatosParaElegir() {
        assertThat(candidatos.content()).isEmpty();
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(error).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesSeGuardaElMiniObjetivoAbandonado() {
        ArgumentCaptor<MiniObjetivo> captor = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoMiniObjetivo.ABANDONADO);
    }

    private void entoncesNoHayPasos() {
        assertThat(pasos.content()).isEmpty();
    }

    private void entoncesSeGuardaLaFotoDelObjetivo() {
        ArgumentCaptor<ProgresoObjetivo> captor = ArgumentCaptor.forClass(ProgresoObjetivo.class);
        verify(progresoObjetivoRepository).registrar(captor.capture());
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    private void entoncesSeGuardaUnaFoto() {
        verify(progresoObjetivoRepository).registrar(any(ProgresoObjetivo.class));
    }

    private void entoncesElProgresoTieneLaMetaYNoTieneSemanas() {
        assertThat(progreso.meta()).isEqualTo(META);
        assertThat(progreso.semanas()).isEmpty();
    }

    private void entoncesSeAnotaLaDecisionDelMiniObjetivo() {
        ArgumentCaptor<DecisionObjetivo> captor = ArgumentCaptor.forClass(DecisionObjetivo.class);
        verify(decisionObjetivoRepository).save(captor.capture());
        assertThat(captor.getValue().miniObjetivoId()).isEqualTo(MINI_ID);
    }

    private void entoncesSeGuardaLaDecisionDelPasoAceptado() {
        ArgumentCaptor<DecisionObjetivo> captor = ArgumentCaptor.forClass(DecisionObjetivo.class);
        verify(decisionObjetivoRepository).save(captor.capture());
        assertThat(captor.getValue().tipo()).isEqualTo(TipoDecision.PASO_ACEPTADO);
    }

    private void entoncesNoHayDecisionesYSeBuscaronLasDelObjetivo() {
        assertThat(decisiones.semanas()).isEmpty();
        verify(decisionObjetivoRepository).findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);
    }

    private void entoncesSeGuardaElResumenDeLaSemana() {
        ArgumentCaptor<ResumenSemanal> captor = ArgumentCaptor.forClass(ResumenSemanal.class);
        verify(resumenSemanalRepository).save(captor.capture());
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    private void entoncesNoHaySemanasYSeBuscaronLasDelObjetivo() {
        assertThat(semanas.semanas()).isEmpty();
        verify(resumenSemanalRepository).findAllByObjetivoId(OBJETIVO_ID);
    }

    private void entoncesSeGuardaLaRespuestaYLaDecision() {
        assertThat(respondida.hizo()).isEqualTo(HizoManual.SI);
        verify(decisionObjetivoRepository).save(any(DecisionObjetivo.class));
    }

    private void entoncesHayUnaRespuestaPendiente() {
        assertThat(pendientes.pendientes()).hasSize(1);
    }

    private void entoncesSeProponeConLaIaYSeReservaConElLimiteConfigurado() {
        assertThat(respuestaDeSami.origen()).isEqualTo(OrigenPropuesta.IA);
        verify(consumoSamiRepository).reservar(eq(NEGOCIO_ID), any(LocalDate.class), eq(LIMITE_DIARIO_CONFIGURADO), any());
    }

    private void entoncesSeProponeElRespaldoSinMensajesRestantes() {
        assertThat(respuestaDeSami.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        assertThat(respuestaDeSami.mensajesRestantes()).isZero();
    }

    private void entoncesSeGuardaElObjetivoManualYSuMini() {
        assertThat(objetivoGuardado.tipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(objetivoGuardado.armadoConSami()).isFalse();
        verify(miniObjetivoRepository).save(any(MiniObjetivo.class));
    }

    private void entoncesSeValidaYSeGastaLaCuota() {
        verify(controlPlanService).validarObjetivoIa(eq(NEGOCIO_ID), any());
        verify(usoPlanRepository).registrarObjetivoIa(eq(NEGOCIO_ID), eq(OBJETIVO_ID), any());
    }

    // --- helpers ---
    private MonitoreoObjetivosService monitoreo() {
        return config.monitoreoObjetivosService(
                objetivoRepository, miniObjetivoRepository, progresoObjetivoRepository, propuesta());
    }

    private PropuestaMiniObjetivosService propuesta() {
        return config.propuestaMiniObjetivosService(
                negocioRepository,
                sistemaRepository,
                datosDeProyectoService,
                zonaRepository,
                agregadoZonaRepository,
                metricas());
    }

    private static MetricasVentas metricas() {
        return new MetricasVentas(30, new BigDecimal("0.5"));
    }
}
