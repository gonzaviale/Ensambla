package com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.RespuestaMiniObjetivoRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.ZoneOffset;

import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.COMENTARIO;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.SEMANA_RESPUESTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResponderMiniObjetivoTest {

    private static final Clock CLOCK = Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final RespuestaMiniObjetivoRepository respuestaMiniObjetivoRepository =
            mock(RespuestaMiniObjetivoRepository.class);
    private final DecisionObjetivoRepository decisionObjetivoRepository = mock(DecisionObjetivoRepository.class);
    private ResponderMiniObjetivo responderMiniObjetivo;
    private ResponderMiniObjetivoResponse respuesta;
    private MiniObjetivo mini;
    private Throwable error;

    @BeforeEach
    void setUp() {
        responderMiniObjetivo = new ResponderMiniObjetivo(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                respuestaMiniObjetivoRepository,
                decisionObjetivoRepository,
                new TransaccionDirecta(),
                CLOCK);
        dadoUnNegocioConSuObjetivo();
    }

    @Test
    @DisplayName("Guarda la respuesta con su fecha")
    void guardaLaRespuestaConSuFecha() {
        dadoUnManualSinResponder();

        cuandoSeResponde(HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO);

        entoncesSeGuardaLaRespuestaConSuFecha();
    }

    @Test
    @DisplayName("Devuelve la respuesta guardada")
    void devuelveLaRespuestaGuardada() {
        dadoUnManualSinResponder();

        cuandoSeResponde(HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO);

        entoncesSeDevuelveLaRespuestaGuardada();
    }

    @Test
    @DisplayName("Anota la decisión en el historial del objetivo antes de guardar la respuesta")
    void anotaLaDecisionEnElHistorialDelObjetivoAntesDeGuardarLaRespuesta() {
        dadoUnManualSinResponder();

        cuandoSeResponde(HizoManual.NO, QuePaso.NO_FUNCIONO);

        entoncesSeAnotaLaDecisionAntesDeGuardarLaRespuesta();
    }

    @Test
    @DisplayName("No cambia el mini-objetivo: sigue en curso hasta que lo renueve el cierre")
    void noCambiaElMiniObjetivoSigueEnCursoHastaQueLoRenueveElCierre() {
        dadoUnManualSinResponder();

        cuandoSeResponde(HizoManual.SI, QuePaso.FUNCIONO);

        entoncesElMiniObjetivoSigueEnCurso();
    }

    @Test
    @DisplayName("Un manual ya respondido responde 409 y no se vuelve a guardar")
    void unManualYaRespondidoResponde409YNoSeVuelveAGuardar() {
        dadoUnManualYaRespondido();

        cuandoSeResponde(HizoManual.SI, QuePaso.FUNCIONO);

        entoncesSeRechazaLaSegundaRespuesta();
    }

    @Test
    @DisplayName("Un mini-objetivo medido no se responde")
    void unMiniObjetivoMedidoNoSeResponde() {
        dadoUnMiniObjetivoMedido();

        cuandoSeResponde(HizoManual.SI, QuePaso.FUNCIONO);

        entoncesSeRechazaPorqueSoloSeRespondenLosManuales();
    }

    @Test
    @DisplayName("Una combinación contradictoria no se guarda")
    void unaCombinacionContradictoriaNoSeGuarda() {
        dadoUnManualSinResponder();

        cuandoSeResponde(HizoManual.SI, QuePaso.NO_TUVE_TIEMPO);

        entoncesSeRechazaLaCombinacionContradictoria();
    }

    @Test
    @DisplayName("Un mini-objetivo de otro objetivo o negocio responde como inexistente")
    void unMiniObjetivoDeOtroObjetivoONegocioRespondeComoInexistente() {
        dadoUnMiniObjetivoInexistente();

        cuandoSeResponde(HizoManual.SI, QuePaso.FUNCIONO);

        entoncesRespondeComoInexistenteSinGuardarNada();
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde como inexistente y no busca el mini-objetivo")
    void unObjetivoDeOtroNegocioRespondeComoInexistenteYNoBuscaElMiniObjetivo() {
        dadoUnObjetivoInexistente();

        cuandoSeResponde(HizoManual.SI, QuePaso.FUNCIONO);

        entoncesRespondeComoInexistenteSinBuscarElMini();
    }

    // --- arrange ---
    private void dadoUnNegocioConSuObjetivo() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
    }

    private void dadoUnManualSinResponder() {
        mini = miniManual();
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, OTRO_MINI_ID)).thenReturn(mini);
        when(respuestaMiniObjetivoRepository.existsByMiniObjetivoId(OTRO_MINI_ID)).thenReturn(false);
        when(respuestaMiniObjetivoRepository.save(any(RespuestaMiniObjetivo.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    private void dadoUnManualYaRespondido() {
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, OTRO_MINI_ID)).thenReturn(miniManual());
        when(respuestaMiniObjetivoRepository.existsByMiniObjetivoId(OTRO_MINI_ID)).thenReturn(true);
    }

    private void dadoUnMiniObjetivoMedido() {
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, OTRO_MINI_ID)).thenReturn(miniObjetivo());
    }

    private void dadoUnMiniObjetivoInexistente() {
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, OTRO_MINI_ID))
                .thenThrow(new ResourceNotFoundException("Mini-objetivo no encontrado"));
    }

    private void dadoUnObjetivoInexistente() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));
    }

    // --- act ---
    private void cuandoSeResponde(HizoManual hizo, QuePaso quePaso) {
        error = catchThrowable(() -> respuesta = responderMiniObjetivo.execute(new ResponderMiniObjetivoRequest(
                USUARIO_ID, OBJETIVO_ID, OTRO_MINI_ID, hizo, quePaso, COMENTARIO)));
    }

    // --- assert ---
    private void entoncesSeGuardaLaRespuestaConSuFecha() {
        RespuestaMiniObjetivo guardada = respuestaGuardada();
        assertThat(guardada.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(guardada.respondidaEn()).isEqualTo(AHORA);
    }

    private void entoncesSeDevuelveLaRespuestaGuardada() {
        assertThat(respuesta.id()).isEqualTo(respuestaGuardada().id());
        assertThat(respuesta.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(respuesta.hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(respuesta.quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(respuesta.comentario()).isEqualTo(COMENTARIO);
        assertThat(respuesta.semanaInicio()).isEqualTo(SEMANA_RESPUESTA);
        assertThat(respuesta.respondidaEn()).isEqualTo(AHORA);
    }

    private void entoncesSeAnotaLaDecisionAntesDeGuardarLaRespuesta() {
        ArgumentCaptor<DecisionObjetivo> decision = ArgumentCaptor.forClass(DecisionObjetivo.class);
        InOrder orden = inOrder(decisionObjetivoRepository, respuestaMiniObjetivoRepository);
        orden.verify(decisionObjetivoRepository).save(decision.capture());
        orden.verify(respuestaMiniObjetivoRepository).save(any(RespuestaMiniObjetivo.class));
        assertThat(decision.getValue().tipo()).isEqualTo(TipoDecision.MINI_NO_HECHO);
        assertThat(decision.getValue().miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(decision.getValue().descripcion()).isEqualTo(TITULO_MANUAL);
    }

    private void entoncesElMiniObjetivoSigueEnCurso() {
        assertThat(mini.estaEnCurso()).isTrue();
        verify(miniObjetivoRepository, never()).save(any());
    }

    private void entoncesSeRechazaLaSegundaRespuesta() {
        assertThat(error)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Ya respondiste este mini-objetivo");
        verify(respuestaMiniObjetivoRepository, never()).save(any());
        verifyNoInteractions(decisionObjetivoRepository);
    }

    private void entoncesSeRechazaPorqueSoloSeRespondenLosManuales() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Sólo se responden los mini-objetivos manuales");
        verifyNoInteractions(respuestaMiniObjetivoRepository, decisionObjetivoRepository);
    }

    private void entoncesSeRechazaLaCombinacionContradictoria() {
        assertThat(error).isInstanceOf(BusinessRuleException.class);
        verify(respuestaMiniObjetivoRepository, never()).save(any());
        verifyNoInteractions(decisionObjetivoRepository);
    }

    private void entoncesRespondeComoInexistenteSinGuardarNada() {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(respuestaMiniObjetivoRepository, decisionObjetivoRepository);
    }

    private void entoncesRespondeComoInexistenteSinBuscarElMini() {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(miniObjetivoRepository, respuestaMiniObjetivoRepository, decisionObjetivoRepository);
    }

    // --- helpers ---
    private RespuestaMiniObjetivo respuestaGuardada() {
        ArgumentCaptor<RespuestaMiniObjetivo> captor = ArgumentCaptor.forClass(RespuestaMiniObjetivo.class);
        verify(respuestaMiniObjetivoRepository).save(captor.capture());
        return captor.getValue();
    }
}
