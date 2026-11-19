package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.CandidatosDelNegocio;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.DecisionPaso;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ProgresoObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ResumenSemanalRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.objetivo.MonitoreoObjetivosService;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.objetivo.abandonarMiniObjetivo.AbandonarMiniObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisionesRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisionesResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivosRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivosResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse;
import com.ensambla.backend.domain.useCase.objetivo.medirObjetivos.MedirObjetivosRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse;
import com.ensambla.backend.domain.useCase.objetivo.registrarDecisionPaso.RegistrarDecisionPasoRequest;
import com.ensambla.backend.domain.useCase.objetivo.reemplazarMiniObjetivo.ReemplazarMiniObjetivoRequest;
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

import static com.ensambla.backend.support.DecisionTestData.aviso;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjetivoUseCaseConfigTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final Clock CLOCK = Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC);

    private final ObjetivoUseCaseConfig config = new ObjetivoUseCaseConfig();
    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final SistemaRepository sistemaRepository = mock(SistemaRepository.class);
    private final ProgresoObjetivoRepository progresoObjetivoRepository = mock(ProgresoObjetivoRepository.class);
    private final DecisionObjetivoRepository decisionObjetivoRepository = mock(DecisionObjetivoRepository.class);
    private final ResumenSemanalRepository resumenSemanalRepository = mock(ResumenSemanalRepository.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);
    private CandidatosDelNegocio candidatosDelNegocio;
    private ListarMiniObjetivosResponse minis;
    private ListarCandidatosResponse candidatos;
    private Throwable error;
    private ObtenerPasosResponse pasos;
    private ObtenerProgresoResponse progreso;
    private ListarDecisionesResponse decisiones;
    private ListarSemanasResponse semanas;

    @Test
    @DisplayName("El wiring de la propuesta carga los sistemas del negocio y no encuentra candidatos sin datos")
    void laPropuestaNoEncuentraCandidatosSinDatos() {
        dadoUnNegocioSinSistemasNiZona();

        cuandoSeBuscanLosCandidatosDelNegocio();

        entoncesElNegocioNoTieneCandidatos();
    }

    @Test
    @DisplayName("El wiring de listar mini-objetivos conecta los repositorios")
    void listarMiniObjetivosConectaLosRepositorios() {
        dadoUnObjetivoConSuMiniObjetivo();

        cuandoSeListanLosMiniObjetivos();

        entoncesSeListoUnMiniObjetivo();
    }

    @Test
    @DisplayName("El wiring de listar candidatos conecta la propuesta y el reloj")
    void listarCandidatosConectaLaPropuestaYElReloj() {
        dadoUnObjetivoConSuMiniObjetivoSinDatos();

        cuandoSeListanLosCandidatos();

        entoncesNoHayCandidatos();
    }

    @Test
    @DisplayName("El wiring de reemplazar conecta la propuesta: sin candidatos el elegido no está disponible")
    void reemplazarSinCandidatosRechazaElElegido() {
        dadoUnObjetivoConSuMiniObjetivoSinDatos();

        cuandoSeReemplazaPorMejorarElMargen();

        entoncesSeRechazaPorUnaReglaDeNegocio();
    }

    @Test
    @DisplayName("El wiring de abandonar conecta los repositorios y guarda el mini-objetivo quitado")
    void abandonarGuardaElMiniObjetivoQuitado() {
        dadoUnObjetivoYSuMiniObjetivo();

        cuandoSeAbandonaElMiniObjetivo();

        entoncesSeGuardoElMiniObjetivoAbandonado();
    }

    @Test
    @DisplayName("El wiring de pasos conecta las fuentes reales del negocio")
    void obtenerPasosConectaLasFuentesDelNegocio() {
        dadoUnUsuarioConNegocioSinSistemasNiZona();

        cuandoSeObtienenLosPasos();

        entoncesNoHayPasos();
    }

    @Test
    @DisplayName("El wiring del monitoreo mide cada objetivo y guarda su foto del día")
    void elMonitoreoGuardaLaFotoDelDiaDeCadaObjetivo() {
        dadoUnNegocioConObjetivos(objetivo());

        cuandoSeMonitoreanLosObjetivos();

        entoncesSeRegistroElProgresoDelObjetivo();
    }

    @Test
    @DisplayName("El wiring de medir mide el negocio de la sesión y guarda su foto del día")
    void medirGuardaLaFotoDelDiaDelNegocioDeLaSesion() {
        dadoUnUsuarioConUnObjetivo();

        cuandoSeMidenLosObjetivos();

        entoncesSeRegistroUnProgreso();
    }

    @Test
    @DisplayName("El wiring del progreso busca el objetivo del negocio de la sesión")
    void elProgresoBuscaElObjetivoDelNegocio() {
        dadoUnObjetivoYSuMiniObjetivoSinSistemas();

        cuandoSeObtieneElProgreso();

        entoncesElProgresoTieneLaMetaSinSemanas();
    }

    @Test
    @DisplayName("El wiring del servicio de decisiones anota una decisión por objetivo activo")
    void elServicioDeDecisionesAnotaUnaPorObjetivoActivo() {
        dadoUnObjetivoActivoDelNegocio();

        cuandoSeRegistraUnAvisoAplicado();

        entoncesLaDecisionEsDelMiniObjetivo();
    }

    @Test
    @DisplayName("El wiring de decidir un paso busca el mini-objetivo del negocio de la sesión y guarda la decisión")
    void decidirUnPasoGuardaLaDecision() {
        dadoUnObjetivoYSuMiniObjetivoQueGuardaDecisiones();

        cuandoSeAceptaElPaso();

        entoncesLaDecisionGuardadaEs(TipoDecision.PASO_ACEPTADO);
    }

    @Test
    @DisplayName("El wiring de listar decisiones busca el objetivo del negocio de la sesión")
    void listarDecisionesBuscaElObjetivoDelNegocio() {
        dadoUnObjetivoYSuMiniObjetivo();

        cuandoSeListanLasDecisiones();

        entoncesNoHaySemanasDeDecisionesBuscadas();
    }

    @Test
    @DisplayName("El wiring del cierre semanal cierra la semana pendiente y guarda su resumen")
    void elCierreSemanalGuardaElResumenPendiente() {
        dadoUnNegocioConObjetivos(unObjetivoDeLaSemanaPasada());

        cuandoSeCierranLasSemanas();

        entoncesSeGuardoElResumenDelObjetivo();
    }

    @Test
    @DisplayName("El wiring de listar semanas busca el objetivo del negocio de la sesión")
    void listarSemanasBuscaElObjetivoDelNegocio() {
        dadoUnObjetivoYSuMiniObjetivo();

        cuandoSeListanLasSemanas();

        entoncesNoHaySemanasBuscadas();
    }

    private void dadoUnNegocioSinSistemasNiZona() {
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnUsuarioConNegocioSinSistemasNiZona() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        dadoUnNegocioSinSistemasNiZona();
    }

    private void dadoUnObjetivoYSuMiniObjetivo() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID)).thenReturn(miniObjetivo());
    }

    private void dadoUnObjetivoConSuMiniObjetivo() {
        dadoUnObjetivoYSuMiniObjetivo();
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(miniObjetivo()));
    }

    private void dadoUnObjetivoConSuMiniObjetivoSinDatos() {
        dadoUnObjetivoConSuMiniObjetivo();
        dadoUnNegocioSinSistemasNiZona();
    }

    private void dadoUnObjetivoYSuMiniObjetivoSinSistemas() {
        dadoUnObjetivoYSuMiniObjetivo();
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
    }

    private void dadoUnObjetivoYSuMiniObjetivoQueGuardaDecisiones() {
        dadoUnObjetivoYSuMiniObjetivo();
        when(decisionObjetivoRepository.save(any(DecisionObjetivo.class))).thenAnswer(guardada -> guardada.getArgument(0));
    }

    private void dadoUnNegocioConObjetivos(Objetivo objetivo) {
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 50)).thenReturn(List.of(NEGOCIO_ID));
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivo));
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
    }

    private void dadoUnUsuarioConUnObjetivo() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivo()));
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
    }

    private void dadoUnObjetivoActivoDelNegocio() {
        Objetivo delNegocio = objetivo();
        delNegocio.setNegocioId(NEGOCIO_ID);
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(delNegocio));
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(miniObjetivo()));
    }

    private void cuandoSeBuscanLosCandidatosDelNegocio() {
        candidatosDelNegocio = propuesta().candidatos(NEGOCIO_ID, LocalDate.of(2026, 11, 10));
    }

    private void cuandoSeListanLosMiniObjetivos() {
        minis = config.listarMiniObjetivos(negocioRepository, objetivoRepository, miniObjetivoRepository)
                .execute(new ListarMiniObjetivosRequest(USUARIO_ID, OBJETIVO_ID));
    }

    private void cuandoSeListanLosCandidatos() {
        candidatos = config.listarCandidatos(negocioRepository, objetivoRepository, miniObjetivoRepository, propuesta(),
                CLOCK).execute(new ListarCandidatosRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID));
    }

    private void cuandoSeReemplazaPorMejorarElMargen() {
        var reemplazar = config.reemplazarMiniObjetivo(negocioRepository, objetivoRepository, miniObjetivoRepository,
                propuesta(), new TransaccionDirecta(), CLOCK);
        error = catchThrowable(() -> reemplazar.execute(new ReemplazarMiniObjetivoRequest(
                USUARIO_ID, OBJETIVO_ID, MINI_ID, TipoMiniObjetivo.MEJORAR_MARGEN, null, null)));
    }

    private void cuandoSeAbandonaElMiniObjetivo() {
        config.abandonarMiniObjetivo(negocioRepository, objetivoRepository, miniObjetivoRepository,
                new TransaccionDirecta(), CLOCK).execute(new AbandonarMiniObjetivoRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID));
    }

    private void cuandoSeObtienenLosPasos() {
        pasos = config.obtenerPasos(negocioRepository, sistemaRepository, mock(DatosDeProyectoService.class),
                        mock(ZonaRepository.class), mock(AgregadoZonaRepository.class), metricasVentas(), Clock.systemUTC())
                .execute(new ObtenerPasosRequest(USUARIO_ID));
    }

    private void cuandoSeMonitoreanLosObjetivos() {
        config.monitorearObjetivos(objetivoRepository, monitoreo(), new TransaccionDirecta(), CLOCK,
                new ObjetivosMonitoreoProperties(50)).execute();
    }

    private void cuandoSeMidenLosObjetivos() {
        config.medirObjetivos(negocioRepository, monitoreo(), new TransaccionDirecta(), CLOCK)
                .execute(new MedirObjetivosRequest(USUARIO_ID));
    }

    private void cuandoSeObtieneElProgreso() {
        progreso = config.obtenerProgreso(negocioRepository, objetivoRepository, miniObjetivoRepository,
                        progresoObjetivoRepository, propuesta(), resumenSemanalRepository, CLOCK)
                .execute(new ObtenerProgresoRequest(USUARIO_ID, OBJETIVO_ID));
    }

    private void cuandoSeRegistraUnAvisoAplicado() {
        Aviso aplicado = aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO);
        aplicado.setNegocioId(NEGOCIO_ID);
        config.decisionesObjetivoService(objetivoRepository, miniObjetivoRepository, decisionObjetivoRepository)
                .registrarAviso(aplicado, TipoDecision.AVISO_APLICADO, AHORA);
    }

    private void cuandoSeAceptaElPaso() {
        config.registrarDecisionPaso(negocioRepository, objetivoRepository, miniObjetivoRepository,
                        decisionObjetivoRepository, new TransaccionDirecta(), CLOCK)
                .execute(new RegistrarDecisionPasoRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID, DecisionPaso.ACEPTADO));
    }

    private void cuandoSeListanLasDecisiones() {
        decisiones = config.listarDecisiones(negocioRepository, objetivoRepository, decisionObjetivoRepository,
                avisoRepository).execute(new ListarDecisionesRequest(USUARIO_ID, OBJETIVO_ID));
    }

    private void cuandoSeCierranLasSemanas() {
        config.cerrarSemanasObjetivos(objetivoRepository, miniObjetivoRepository, decisionObjetivoRepository,
                avisoRepository, resumenSemanalRepository, propuesta(), new TransaccionDirecta(), CLOCK,
                new ObjetivosCierreProperties(8, 50)).execute();
    }

    private void cuandoSeListanLasSemanas() {
        semanas = config.listarSemanas(negocioRepository, objetivoRepository, resumenSemanalRepository)
                .execute(new ListarSemanasRequest(USUARIO_ID, OBJETIVO_ID));
    }

    private void entoncesElNegocioNoTieneCandidatos() {
        assertThat(candidatosDelNegocio.candidatos()).isEmpty();
    }

    private void entoncesSeListoUnMiniObjetivo() {
        assertThat(minis.minis()).hasSize(1);
    }

    private void entoncesNoHayCandidatos() {
        assertThat(candidatos.content()).isEmpty();
    }

    private void entoncesSeRechazaPorUnaReglaDeNegocio() {
        assertThat(error).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesSeGuardoElMiniObjetivoAbandonado() {
        ArgumentCaptor<MiniObjetivo> guardado = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getEstado()).isEqualTo(EstadoMiniObjetivo.ABANDONADO);
    }

    private void entoncesNoHayPasos() {
        assertThat(pasos.content()).isEmpty();
    }

    private void entoncesSeRegistroElProgresoDelObjetivo() {
        ArgumentCaptor<ProgresoObjetivo> registrado = ArgumentCaptor.forClass(ProgresoObjetivo.class);
        verify(progresoObjetivoRepository).registrar(registrado.capture());
        assertThat(registrado.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    private void entoncesSeRegistroUnProgreso() {
        verify(progresoObjetivoRepository).registrar(any(ProgresoObjetivo.class));
    }

    private void entoncesElProgresoTieneLaMetaSinSemanas() {
        assertThat(progreso.meta()).isEqualTo(META);
        assertThat(progreso.semanas()).isEmpty();
    }

    private void entoncesLaDecisionEsDelMiniObjetivo() {
        assertThat(decisionGuardada().miniObjetivoId()).isEqualTo(MINI_ID);
    }

    private void entoncesLaDecisionGuardadaEs(TipoDecision tipo) {
        assertThat(decisionGuardada().tipo()).isEqualTo(tipo);
    }

    private void entoncesNoHaySemanasDeDecisionesBuscadas() {
        assertThat(decisiones.semanas()).isEmpty();
        verify(decisionObjetivoRepository).findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);
    }

    private void entoncesSeGuardoElResumenDelObjetivo() {
        ArgumentCaptor<ResumenSemanal> guardado = ArgumentCaptor.forClass(ResumenSemanal.class);
        verify(resumenSemanalRepository).save(guardado.capture());
        assertThat(guardado.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    private void entoncesNoHaySemanasBuscadas() {
        assertThat(semanas.semanas()).isEmpty();
        verify(resumenSemanalRepository).findAllByObjetivoId(OBJETIVO_ID);
    }

    private DecisionObjetivo decisionGuardada() {
        ArgumentCaptor<DecisionObjetivo> guardada = ArgumentCaptor.forClass(DecisionObjetivo.class);
        verify(decisionObjetivoRepository).save(guardada.capture());
        return guardada.getValue();
    }

    private static Objetivo unObjetivoDeLaSemanaPasada() {
        Objetivo deLaSemanaPasada = objetivo();
        deLaSemanaPasada.setCreadoEn(AHORA.minusWeeks(1));
        return deLaSemanaPasada;
    }

    private MonitoreoObjetivosService monitoreo() {
        return config.monitoreoObjetivosService(objetivoRepository, miniObjetivoRepository, progresoObjetivoRepository,
                propuesta());
    }

    private PropuestaMiniObjetivosService propuesta() {
        return config.propuestaMiniObjetivosService(negocioRepository, sistemaRepository,
                mock(DatosDeProyectoService.class), mock(ZonaRepository.class), mock(AgregadoZonaRepository.class),
                metricasVentas());
    }

    private static MetricasVentas metricasVentas() {
        return new MetricasVentas(30, new BigDecimal("0.5"));
    }
}