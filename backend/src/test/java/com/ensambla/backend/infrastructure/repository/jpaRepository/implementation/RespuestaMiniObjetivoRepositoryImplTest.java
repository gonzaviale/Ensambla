package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.RespuestaMiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRespuestaMiniObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.RespuestaMiniObjetivoRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.RESPUESTA_ID;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.respuesta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RespuestaMiniObjetivoRepositoryImplTest {

    private final IRespuestaMiniObjetivoJpaRepository jpa = mock(IRespuestaMiniObjetivoJpaRepository.class);
    private final RespuestaMiniObjetivoRepositoryMapper mapper = new RespuestaMiniObjetivoRepositoryMapper();
    private final RespuestaMiniObjetivoRepositoryImpl repository = new RespuestaMiniObjetivoRepositoryImpl(jpa, mapper);

    @Test
    @DisplayName("Guarda escribiendo en el acto y devuelve lo guardado como dominio")
    void saveShouldWriteImmediatelyAndMapTheResult() {
        when(jpa.saveAndFlush(any(RespuestaMiniObjetivoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RespuestaMiniObjetivo result = repository.save(respuesta());

        ArgumentCaptor<RespuestaMiniObjetivoEntity> captor =
                ArgumentCaptor.forClass(RespuestaMiniObjetivoEntity.class);
        verify(jpa).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(RESPUESTA_ID.valor());
        assertThat(result.id()).isEqualTo(RESPUESTA_ID);
    }

    @Test
    @DisplayName("Pregunta por el mini-objetivo con el UUID de su id")
    void existsByMiniObjetivoIdShouldAskWithTheUuid() {
        when(jpa.existsByMiniObjetivoId(OTRO_MINI_ID.valor())).thenReturn(true);

        assertThat(repository.existsByMiniObjetivoId(OTRO_MINI_ID)).isTrue();
    }

    @Test
    @DisplayName("Lista las respuestas del objetivo del negocio con los UUID de los ids")
    void findAllByObjetivoIdAndNegocioIdShouldMapTheAnswers() {
        when(jpa.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(List.of(mapper.toEntity(respuesta())));

        List<RespuestaMiniObjetivo> result = repository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);

        assertThat(result).singleElement().satisfies(respuesta -> {
            assertThat(respuesta.id()).isEqualTo(RESPUESTA_ID);
            assertThat(respuesta.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        });
    }

    @Test
    @DisplayName("Sin respuestas devuelve una lista vacía")
    void findAllByObjetivoIdAndNegocioIdShouldReturnAnEmptyList() {
        when(jpa.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID.valor(), NEGOCIO_ID.valor())).thenReturn(List.of());

        assertThat(repository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID)).isEmpty();
    }
}
