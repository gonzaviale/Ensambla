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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @BeforeEach
    void setUp() {
        registrarDecisionPaso = new RegistrarDecisionPaso(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                decisionObjetivoRepository,
                new TransaccionDirecta(),
                CLOCK);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(decisionObjetivoRepository.save(any(DecisionObjetivo.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @DisplayName("Anota que la persona aceptó el paso del mini-objetivo en la semana de hoy")
    void executeShouldRecordTheAcceptedStep() {
        givenMini(miniObjetivo());

        DecisionItem result = whenDecidir(DecisionPaso.ACEPTADO);

        ArgumentCaptor<DecisionObjetivo> captor = ArgumentCaptor.forClass(DecisionObjetivo.class);
        verify(decisionObjetivoRepository).save(captor.capture());
        assertThat(captor.getValue().tipo()).isEqualTo(TipoDecision.PASO_ACEPTADO);
        assertThat(captor.getValue().miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(captor.getValue().tomadaEn()).isEqualTo(AHORA);
        assertThat(captor.getValue().semanaInicio()).isEqualTo(SEMANA);
        assertThat(result.tipo()).isEqualTo(TipoDecision.PASO_ACEPTADO);
        assertThat(result.descripcion()).isEqualTo(TITULO);
        assertThat(result.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.efecto()).isEqualTo(EstadoEfecto.SIN_MEDICION);
    }

    @Test
    @DisplayName("Anota que la persona dijo «ahora no» al paso")
    void executeShouldRecordTheIgnoredStep() {
        givenMini(miniObjetivo());

        DecisionItem result = whenDecidir(DecisionPaso.IGNORADO);

        assertThat(result.tipo()).isEqualTo(TipoDecision.PASO_IGNORADO);
    }

    @Test
    @DisplayName("No se decide el paso de un mini-objetivo que ya no está en curso")
    void executeShouldRejectAMiniObjetivoNoLongerInProgress() {
        MiniObjetivo logrado = miniObjetivo();
        logrado.setEstado(EstadoMiniObjetivo.LOGRADO);
        givenMini(logrado);

        assertThatThrownBy(() -> whenDecidir(DecisionPaso.ACEPTADO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mini-objetivo ya no está en curso");
        verify(decisionObjetivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un mini-objetivo de otro objetivo o negocio responde como inexistente")
    void executeShouldFailWhenTheMiniObjetivoIsNotFound() {
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID))
                .thenThrow(new ResourceNotFoundException("Mini-objetivo no encontrado"));

        assertThatThrownBy(() -> whenDecidir(DecisionPaso.ACEPTADO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Mini-objetivo no encontrado");
        verifyNoInteractions(decisionObjetivoRepository);
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde como inexistente y no busca el mini-objetivo")
    void executeShouldFailWhenTheObjetivoIsNotFound() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));

        assertThatThrownBy(() -> whenDecidir(DecisionPaso.ACEPTADO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Objetivo no encontrado");
        verifyNoInteractions(miniObjetivoRepository, decisionObjetivoRepository);
    }

    // --- arrange ---
    private void givenMini(MiniObjetivo mini) {
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID)).thenReturn(mini);
    }

    // --- act ---
    private DecisionItem whenDecidir(DecisionPaso decision) {
        return registrarDecisionPaso.execute(new RegistrarDecisionPasoRequest(
                USUARIO_ID, OBJETIVO_ID, MINI_ID, decision));
    }
}
