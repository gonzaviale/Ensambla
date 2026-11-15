package com.ensambla.backend.domain.useCase.objetivo.listarDecisiones;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.objetivo.EstadoEfecto;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.aviso;
import static com.ensambla.backend.support.DecisionTestData.decision;
import static com.ensambla.backend.support.DecisionTestData.impacto;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarDecisionesTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final DecisionObjetivoRepository decisionObjetivoRepository = mock(DecisionObjetivoRepository.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);
    private ListarDecisiones listarDecisiones;

    @BeforeEach
    void setUp() {
        listarDecisiones = new ListarDecisiones(
                negocioRepository, objetivoRepository, decisionObjetivoRepository, avisoRepository);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
    }

    @Test
    @DisplayName("Trae las decisiones del objetivo con el efecto de los avisos, leyendo los avisos de una vez")
    void executeShouldReturnTheDecisionsWithTheEffectOfTheAvisos() {
        Aviso aplicado = aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO);
        aplicado.registrarImpacto(impacto(), AHORA);
        when(decisionObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(decision(TipoDecision.AVISO_APLICADO, AVISO_ID)));
        when(avisoRepository.findAllByIdsAndNegocioId(List.of(AVISO_ID), NEGOCIO_ID)).thenReturn(List.of(aplicado));

        ListarDecisionesResponse result = whenListar();

        assertThat(result.semanas()).singleElement().satisfies(semana -> assertThat(semana.decisiones())
                .singleElement().satisfies(decision -> {
                    assertThat(decision.efecto()).isEqualTo(EstadoEfecto.MEDIDO);
                    assertThat(decision.deltaIngresoSemanal()).isEqualByComparingTo("350.00");
                }));
    }

    @Test
    @DisplayName("Sin decisiones de avisos no consulta los avisos")
    void executeShouldNotQueryTheAvisosWhenThereAreNoAvisoDecisions() {
        when(decisionObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(decision(TipoDecision.PASO_ACEPTADO, null)));

        ListarDecisionesResponse result = whenListar();

        assertThat(result.semanas()).singleElement()
                .satisfies(semana -> assertThat(semana.decisiones()).hasSize(1));
        verifyNoInteractions(avisoRepository);
    }

    @Test
    @DisplayName("Sin decisiones devuelve una lista vacía")
    void executeShouldReturnEmptyWithoutDecisions() {
        when(decisionObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of());

        assertThat(whenListar().semanas()).isEmpty();
    }

    @Test
    @DisplayName("Un aviso borrado deja su decisión sin medición en lugar de fallar")
    void executeShouldKeepTheDecisionWhenTheAvisoWasDeleted() {
        when(decisionObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(decision(TipoDecision.AVISO_APLICADO, AVISO_ID)));
        when(avisoRepository.findAllByIdsAndNegocioId(List.of(AVISO_ID), NEGOCIO_ID)).thenReturn(List.of());

        ListarDecisionesResponse result = whenListar();

        assertThat(result.semanas().getFirst().decisiones().getFirst().efecto())
                .isEqualTo(EstadoEfecto.SIN_MEDICION);
        verify(avisoRepository).findAllByIdsAndNegocioId(List.of(AVISO_ID), NEGOCIO_ID);
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde como inexistente y no consulta sus decisiones")
    void executeShouldFailWhenTheObjetivoIsNotFound() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));

        assertThatThrownBy(this::whenListar)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Objetivo no encontrado");
        verifyNoInteractions(decisionObjetivoRepository, avisoRepository);
    }

    // --- act ---
    private ListarDecisionesResponse whenListar() {
        return listarDecisiones.execute(new ListarDecisionesRequest(USUARIO_ID, OBJETIVO_ID));
    }
}
