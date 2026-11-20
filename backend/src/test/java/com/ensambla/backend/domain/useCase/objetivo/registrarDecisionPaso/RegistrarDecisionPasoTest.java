package com.ensambla.backend.domain.useCase.objetivo.registrarDecisionPaso;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.DecisionPaso;
import com.ensambla.backend.domain.model.objetivo.EstadoEfecto;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.useCase.objetivo.DecisionItem;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;

import static com.ensambla.backend.support.DecisionTestData.SEMANA;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegistrarDecisionPasoTest {

    private static final Clock CLOCK = Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final DecisionObjetivoRepository decisionObjetivoRepository = mock(DecisionObjetivoRepository.class);
    private RegistrarDecisionPaso registrarDecisionPaso;

    private DecisionItem decisionRegistrada;
    private Throwable rechazo;

    @BeforeEach
    void setUp() {
        registrarDecisionPaso = new RegistrarDecisionPaso(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                decisionObjetivoRepository,
                new TransaccionDirecta(),
                CLOCK);
        dadoUnObjetivoDelNegocio();
    }

    @Test
    @DisplayName("Anota que la persona aceptó el paso del mini-objetivo en la semana de hoy")
    void anotaQueLaPersonaAceptoElPasoDelMiniObjetivoEnLaSemanaDeHoy() {
        dadoUnMiniObjetivoEnCurso();

        cuandoLaPersonaDecide(DecisionPaso.ACEPTADO);

        entoncesSeGuardaLaDecisionAceptadaDeLaSemanaDeHoy();
        entoncesSeDevuelveLaDecisionSinMedicion();
    }

    @Test
    @DisplayName("Anota que la persona dijo «ahora no» al paso")
    void anotaQueLaPersonaDijoAhoraNoAlPaso() {
        dadoUnMiniObjetivoEnCurso();

        cuandoLaPersonaDecide(DecisionPaso.IGNORADO);

        entoncesSeAnotaElPasoComoIgnorado();
    }

    @Test
    @DisplayName("No se decide el paso de un mini-objetivo que ya no está en curso")
    void noSeDecideElPasoDeUnMiniObjetivoQueYaNoEstaEnCurso() {
        dadoUnMiniObjetivoLogrado();

        cuandoLaPersonaIntentaDecidir(DecisionPaso.ACEPTADO);

        entoncesSeRechazaPorqueYaNoEstaEnCursoSinGuardar();
    }

    @Test
    @DisplayName("Un mini-objetivo de otro objetivo o negocio responde como inexistente")
    void unMiniObjetivoDeOtroObjetivoONegocioRespondeComoInexistente() {
        dadoUnMiniObjetivoAjeno();

        cuandoLaPersonaIntentaDecidir(DecisionPaso.ACEPTADO);

        entoncesSeRechazaComoInexistenteSinGuardar("Mini-objetivo no encontrado");
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde como inexistente y no busca el mini-objetivo")
    void unObjetivoDeOtroNegocioRespondeComoInexistenteYNoBuscaElMiniObjetivo() {
        dadoUnObjetivoAjeno();

        cuandoLaPersonaIntentaDecidir(DecisionPaso.ACEPTADO);

        entoncesSeRechazaComoInexistenteSinBuscarElMiniObjetivo();
    }

    // --- dado ---

    private void dadoUnObjetivoDelNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(decisionObjetivoRepository.save(any(DecisionObjetivo.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    private void dadoUnMiniObjetivoEnCurso() {
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID)).thenReturn(miniObjetivo());
    }

    private void dadoUnMiniObjetivoLogrado() {
        MiniObjetivo logrado = miniObjetivo();
        logrado.setEstado(EstadoMiniObjetivo.LOGRADO);
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID)).thenReturn(logrado);
    }

    private void dadoUnMiniObjetivoAjeno() {
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID))
                .thenThrow(new ResourceNotFoundException("Mini-objetivo no encontrado"));
    }

    private void dadoUnObjetivoAjeno() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));
    }

    // --- cuando ---

    private void cuandoLaPersonaDecide(DecisionPaso decision) {
        decisionRegistrada = registrarDecisionPaso.execute(new RegistrarDecisionPasoRequest(
                USUARIO_ID, OBJETIVO_ID, MINI_ID, decision));
    }

    private void cuandoLaPersonaIntentaDecidir(DecisionPaso decision) {
        rechazo = catchThrowable(() -> cuandoLaPersonaDecide(decision));
    }

    // --- entonces ---

    private void entoncesSeGuardaLaDecisionAceptadaDeLaSemanaDeHoy() {
        ArgumentCaptor<DecisionObjetivo> captor = ArgumentCaptor.forClass(DecisionObjetivo.class);
        verify(decisionObjetivoRepository).save(captor.capture());
        assertThat(captor.getValue().tipo()).isEqualTo(TipoDecision.PASO_ACEPTADO);
        assertThat(captor.getValue().miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(captor.getValue().tomadaEn()).isEqualTo(AHORA);
        assertThat(captor.getValue().semanaInicio()).isEqualTo(SEMANA);
    }

    private void entoncesSeDevuelveLaDecisionSinMedicion() {
        assertThat(decisionRegistrada.tipo()).isEqualTo(TipoDecision.PASO_ACEPTADO);
        assertThat(decisionRegistrada.descripcion()).isEqualTo(TITULO);
        assertThat(decisionRegistrada.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(decisionRegistrada.efecto()).isEqualTo(EstadoEfecto.SIN_MEDICION);
    }

    private void entoncesSeAnotaElPasoComoIgnorado() {
        assertThat(decisionRegistrada.tipo()).isEqualTo(TipoDecision.PASO_IGNORADO);
    }

    private void entoncesSeRechazaPorqueYaNoEstaEnCursoSinGuardar() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mini-objetivo ya no está en curso");
        verify(decisionObjetivoRepository, never()).save(any());
    }

    private void entoncesSeRechazaComoInexistenteSinGuardar(String mensaje) {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage(mensaje);
        verifyNoInteractions(decisionObjetivoRepository);
    }

    private void entoncesSeRechazaComoInexistenteSinBuscarElMiniObjetivo() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("Objetivo no encontrado");
        verifyNoInteractions(miniObjetivoRepository, decisionObjetivoRepository);
    }
}
