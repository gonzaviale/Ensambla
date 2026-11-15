package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.infrastructure.repository.entity.DecisionObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IDecisionObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.DecisionObjetivoRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.DECISION_ID;
import static com.ensambla.backend.support.DecisionTestData.OTRA_DECISION_ID;
import static com.ensambla.backend.support.DecisionTestData.SEMANA;
import static com.ensambla.backend.support.DecisionTestData.decision;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DecisionObjetivoRepositoryImplTest {

    private final IDecisionObjetivoJpaRepository jpa = mock(IDecisionObjetivoJpaRepository.class);
    private final DecisionObjetivoRepositoryImpl repository =
            new DecisionObjetivoRepositoryImpl(jpa, new DecisionObjetivoRepositoryMapper());

    @Test
    @DisplayName("Guarda la entidad armada desde el dominio y devuelve lo persistido")
    void saveShouldPersistAndReturnSaved() {
        when(jpa.save(any(DecisionObjetivoEntity.class))).thenReturn(entity(DECISION_ID.valor()));

        DecisionObjetivo result = repository.save(decision(TipoDecision.AVISO_APLICADO, AVISO_ID));

        ArgumentCaptor<DecisionObjetivoEntity> captor = ArgumentCaptor.forClass(DecisionObjetivoEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getObjetivoId()).isEqualTo(OBJETIVO_ID.valor());
        assertThat(captor.getValue().getAvisoId()).isEqualTo(AVISO_ID.valor());
        assertThat(result.id()).isEqualTo(DECISION_ID);
    }

    @Test
    @DisplayName("Lista las decisiones del objetivo en el orden de la consulta, con los UUID, y las mapea al dominio")
    void findAllByObjetivoIdAndNegocioIdShouldMapInOrder() {
        when(jpa.findAllByObjetivoIdAndNegocioIdOrderByTomadaEnDescIdDesc(OBJETIVO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(List.of(entity(DECISION_ID.valor()), entity(OTRA_DECISION_ID.valor())));

        List<DecisionObjetivo> result = repository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);

        assertThat(result).extracting(DecisionObjetivo::id).containsExactly(DECISION_ID, OTRA_DECISION_ID);
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando el objetivo no tiene decisiones")
    void findAllByObjetivoIdAndNegocioIdShouldReturnEmpty() {
        when(jpa.findAllByObjetivoIdAndNegocioIdOrderByTomadaEnDescIdDesc(OBJETIVO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(List.of());

        assertThat(repository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID)).isEmpty();
    }

    // --- helpers ---
    private static DecisionObjetivoEntity entity(UUID id) {
        return DecisionObjetivoEntity.builder()
                .id(id)
                .negocioId(NEGOCIO_ID.valor())
                .objetivoId(OBJETIVO_ID.valor())
                .tipo(TipoDecision.AVISO_APLICADO)
                .descripcion("Aviso")
                .tomadaEn(AHORA)
                .semanaInicio(SEMANA)
                .build();
    }
}
