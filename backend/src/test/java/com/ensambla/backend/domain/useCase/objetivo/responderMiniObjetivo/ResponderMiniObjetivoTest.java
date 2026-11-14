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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
    }

    @Test
    @DisplayName("Guarda la respuesta con su fecha y la devuelve")
    void executeShouldSaveAndReturnTheAnswer() {
        givenMini(miniManual());
        when(respuestaMiniObjetivoRepository.existsByMiniObjetivoId(OTRO_MINI_ID)).thenReturn(false);
        when(respuestaMiniObjetivoRepository.save(any(RespuestaMiniObjetivo.class)))
                .thenAnswer(call -> call.getArgument(0));

        ResponderMiniObjetivoResponse result = whenResponder(HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO);

        ArgumentCaptor<RespuestaMiniObjetivo> captor = ArgumentCaptor.forClass(RespuestaMiniObjetivo.class);
        verify(respuestaMiniObjetivoRepository).save(captor.capture());
        assertThat(captor.getValue().miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(captor.getValue().respondidaEn()).isEqualTo(AHORA);
        assertThat(result.id()).isEqualTo(captor.getValue().id());
        assertThat(result.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(result.hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(result.quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(result.comentario()).isEqualTo(COMENTARIO);
        assertThat(result.semanaInicio()).isEqualTo(SEMANA_RESPUESTA);
        assertThat(result.respondidaEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Anota la decisión en el historial del objetivo antes de guardar la respuesta")
    void executeShouldRecordTheDecisionBeforeSavingTheAnswer() {
        givenMini(miniManual());
        when(respuestaMiniObjetivoRepository.save(any(RespuestaMiniObjetivo.class)))
                .thenAnswer(call -> call.getArgument(0));

        whenResponder(HizoManual.NO, QuePaso.NO_FUNCIONO);

        ArgumentCaptor<DecisionObjetivo> decision = ArgumentCaptor.forClass(DecisionObjetivo.class);
        InOrder orden = inOrder(decisionObjetivoRepository, respuestaMiniObjetivoRepository);
        orden.verify(decisionObjetivoRepository).save(decision.capture());
        orden.verify(respuestaMiniObjetivoRepository).save(any(RespuestaMiniObjetivo.class));
        assertThat(decision.getValue().tipo()).isEqualTo(TipoDecision.MINI_NO_HECHO);
        assertThat(decision.getValue().miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(decision.getValue().descripcion()).isEqualTo(TITULO_MANUAL);
    }

    @Test
    @DisplayName("No cambia el mini-objetivo: sigue en curso hasta que lo renueve el cierre")
    void executeShouldNotChangeTheMiniObjetivo() {
        MiniObjetivo mini = miniManual();
        givenMini(mini);
        when(respuestaMiniObjetivoRepository.save(any(RespuestaMiniObjetivo.class)))
                .thenAnswer(call -> call.getArgument(0));

        whenResponder(HizoManual.SI, QuePaso.FUNCIONO);

        assertThat(mini.estaEnCurso()).isTrue();
        verify(miniObjetivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un manual ya respondido responde 409 y no se vuelve a guardar")
    void executeShouldRejectASecondAnswer() {
        givenMini(miniManual());
        when(respuestaMiniObjetivoRepository.existsByMiniObjetivoId(OTRO_MINI_ID)).thenReturn(true);

        assertThatThrownBy(() -> whenResponder(HizoManual.SI, QuePaso.FUNCIONO))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Ya respondiste este mini-objetivo");
        verify(respuestaMiniObjetivoRepository, never()).save(any());
        verifyNoInteractions(decisionObjetivoRepository);
    }

    @Test
    @DisplayName("Un mini-objetivo medido no se responde")
    void executeShouldRejectAMeasuredMiniObjetivo() {
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, OTRO_MINI_ID)).thenReturn(miniObjetivo());

        assertThatThrownBy(() -> whenResponder(HizoManual.SI, QuePaso.FUNCIONO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Sólo se responden los mini-objetivos manuales");
        verifyNoInteractions(respuestaMiniObjetivoRepository, decisionObjetivoRepository);
    }

    @Test
    @DisplayName("Una combinación contradictoria no se guarda")
    void executeShouldRejectAContradictoryAnswer() {
        givenMini(miniManual());

        assertThatThrownBy(() -> whenResponder(HizoManual.SI, QuePaso.NO_TUVE_TIEMPO))
                .isInstanceOf(BusinessRuleException.class);
        verify(respuestaMiniObjetivoRepository, never()).save(any());
        verifyNoInteractions(decisionObjetivoRepository);
    }

    @Test
    @DisplayName("Un mini-objetivo de otro objetivo o negocio responde como inexistente")
    void executeShouldFailWhenTheMiniObjetivoIsNotFound() {
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, OTRO_MINI_ID))
                .thenThrow(new ResourceNotFoundException("Mini-objetivo no encontrado"));

        assertThatThrownBy(() -> whenResponder(HizoManual.SI, QuePaso.FUNCIONO))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(respuestaMiniObjetivoRepository, decisionObjetivoRepository);
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde como inexistente y no busca el mini-objetivo")
    void executeShouldFailWhenTheObjetivoIsNotFound() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));

        assertThatThrownBy(() -> whenResponder(HizoManual.SI, QuePaso.FUNCIONO))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(miniObjetivoRepository, respuestaMiniObjetivoRepository, decisionObjetivoRepository);
    }

    // --- arrange ---
    private void givenMini(MiniObjetivo mini) {
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, OTRO_MINI_ID)).thenReturn(mini);
    }

    // --- act ---
    private ResponderMiniObjetivoResponse whenResponder(HizoManual hizo, QuePaso quePaso) {
        return responderMiniObjetivo.execute(new ResponderMiniObjetivoRequest(
                USUARIO_ID, OBJETIVO_ID, OTRO_MINI_ID, hizo, quePaso, COMENTARIO));
    }
}
