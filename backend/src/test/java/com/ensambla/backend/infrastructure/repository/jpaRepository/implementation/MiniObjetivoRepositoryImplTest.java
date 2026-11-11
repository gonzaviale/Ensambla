package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMiniObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.MiniObjetivoRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MiniObjetivoRepositoryImplTest {

    private final IMiniObjetivoJpaRepository jpa = mock(IMiniObjetivoJpaRepository.class);
    private final MiniObjetivoRepositoryMapper mapper = new MiniObjetivoRepositoryMapper();
    private final MiniObjetivoRepositoryImpl repository = new MiniObjetivoRepositoryImpl(jpa, mapper);

    @Test
    @DisplayName("Lista los mini-objetivos del objetivo del negocio, en el orden de la base, con los UUID de los ids")
    void findAllByObjetivoIdAndNegocioIdShouldMapInOrder() {
        MiniObjetivo otro = miniObjetivo();
        otro.setId(OTRO_MINI_ID);
        when(jpa.findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(
                OBJETIVO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(List.of(mapper.toEntity(miniObjetivo()), mapper.toEntity(otro)));

        List<MiniObjetivo> result = repository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);

        assertThat(result).extracting(MiniObjetivo::getId).containsExactly(MINI_ID, OTRO_MINI_ID);
    }

    @Test
    @DisplayName("Sin mini-objetivos devuelve una lista vacía")
    void findAllByObjetivoIdAndNegocioIdShouldReturnEmptyList() {
        when(jpa.findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(
                OBJETIVO_ID.valor(), NEGOCIO_ID.valor())).thenReturn(List.of());

        assertThat(repository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID)).isEmpty();
    }

    @Test
    @DisplayName("Busca por id, objetivo y negocio y mapea el resultado")
    void findByIdAndObjetivoIdAndNegocioIdShouldMapTheEntity() {
        when(jpa.findByIdAndObjetivoIdAndNegocioId(MINI_ID.valor(), OBJETIVO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(mapper.toEntity(miniObjetivo())));

        Optional<MiniObjetivo> result = repository.findByIdAndObjetivoIdAndNegocioId(MINI_ID, OBJETIVO_ID, NEGOCIO_ID);

        assertThat(result).hasValueSatisfying(mini -> assertThat(mini.getId()).isEqualTo(MINI_ID));
    }

    @Test
    @DisplayName("Uno de otro objetivo o de otro negocio no se encuentra")
    void findByIdAndObjetivoIdAndNegocioIdShouldReturnEmptyWhenMissing() {
        when(jpa.findByIdAndObjetivoIdAndNegocioId(MINI_ID.valor(), OBJETIVO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.empty());

        assertThat(repository.findByIdAndObjetivoIdAndNegocioId(MINI_ID, OBJETIVO_ID, NEGOCIO_ID)).isEmpty();
    }

    @Test
    @DisplayName("Guarda escribiendo en el acto y devuelve lo guardado como dominio")
    void saveShouldWriteImmediatelyAndMapTheResult() {
        when(jpa.saveAndFlush(any(MiniObjetivoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MiniObjetivo result = repository.save(miniObjetivo());

        ArgumentCaptor<MiniObjetivoEntity> captor = ArgumentCaptor.forClass(MiniObjetivoEntity.class);
        verify(jpa).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(MINI_ID.valor());
        assertThat(result.getId()).isEqualTo(MiniObjetivoId.de(MINI_ID.valor()));
    }
}
