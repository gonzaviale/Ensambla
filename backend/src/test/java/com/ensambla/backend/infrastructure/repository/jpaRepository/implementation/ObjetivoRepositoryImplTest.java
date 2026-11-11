package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ObjetivoRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjetivoRepositoryImplTest {

    private static final UUID OTRO_ID = UUID.fromString("00000000-0000-4000-8000-000000000006");

    private final IObjetivoJpaRepository jpa = mock(IObjetivoJpaRepository.class);
    private final ObjetivoRepositoryImpl repository = new ObjetivoRepositoryImpl(jpa, new ObjetivoRepositoryMapper());

    @Test
    @DisplayName("Lista los objetivos del negocio en el orden de la consulta y los mapea al dominio")
    void findAllByNegocioIdShouldMapInOrder() {
        when(jpa.findAllByNegocioIdOrderByCreadoEnAscIdAsc(NEGOCIO_ID.valor()))
                .thenReturn(List.of(entity(OBJETIVO_ID.valor()), entity(OTRO_ID)));

        List<Objetivo> result = repository.findAllByNegocioId(NEGOCIO_ID);

        assertThat(result).extracting(Objetivo::getId)
                .extracting(id -> id.valor())
                .containsExactly(OBJETIVO_ID.valor(), OTRO_ID);
        assertThat(result.getFirst().getTexto()).isEqualTo(TEXTO);
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando el negocio no tiene objetivos")
    void findAllByNegocioIdShouldReturnEmpty() {
        when(jpa.findAllByNegocioIdOrderByCreadoEnAscIdAsc(NEGOCIO_ID.valor())).thenReturn(List.of());

        List<Objetivo> result = repository.findAllByNegocioId(NEGOCIO_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Busca por id y negocio con los UUID y mapea el resultado")
    void findByIdAndNegocioIdShouldMapPresent() {
        when(jpa.findByIdAndNegocioId(OBJETIVO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(entity(OBJETIVO_ID.valor())));

        Optional<Objetivo> result = repository.findByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);

        assertThat(result).map(Objetivo::getId).contains(OBJETIVO_ID);
    }

    @Test
    @DisplayName("Devuelve vacío cuando el objetivo no es del negocio")
    void findByIdAndNegocioIdShouldReturnEmptyWhenAbsent() {
        when(jpa.findByIdAndNegocioId(OBJETIVO_ID.valor(), NEGOCIO_ID.valor())).thenReturn(Optional.empty());

        Optional<Objetivo> result = repository.findByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Guarda la entidad armada desde el dominio y devuelve lo persistido")
    void saveShouldPersistAndReturnSaved() {
        when(jpa.save(any(ObjetivoEntity.class))).thenReturn(entity(OBJETIVO_ID.valor()));

        Objetivo result = repository.save(Objetivo.nuevo(NEGOCIO_ID, TEXTO, META, AHORA));

        ArgumentCaptor<ObjetivoEntity> captor = ArgumentCaptor.forClass(ObjetivoEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(captor.getValue().getTexto()).isEqualTo(TEXTO);
        assertThat(result.getId()).isEqualTo(OBJETIVO_ID);
    }

    @Test
    @DisplayName("Borra pasando al JPA los UUID del objetivo y del negocio")
    void deleteByIdAndNegocioIdShouldPassUuids() {
        repository.deleteByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);

        verify(jpa).deleteByIdAndNegocioId(OBJETIVO_ID.valor(), NEGOCIO_ID.valor());
    }

    // --- helpers ---
    private ObjetivoEntity entity(UUID id) {
        return ObjetivoEntity.builder()
                .id(id)
                .negocioId(NEGOCIO_ID.valor())
                .texto(TEXTO)
                .meta(META)
                .creadoEn(AHORA)
                .build();
    }
}
