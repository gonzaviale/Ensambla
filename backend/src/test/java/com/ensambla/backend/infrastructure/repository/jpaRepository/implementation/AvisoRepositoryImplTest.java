package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.AvisoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IAvisoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.AvisoRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvisoRepositoryImplTest {

    private static final UUID ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");

    private final IAvisoJpaRepository jpa = mock(IAvisoJpaRepository.class);
    private final AvisoRepositoryImpl repository = new AvisoRepositoryImpl(jpa, new AvisoRepositoryMapper());

    @Test
    @DisplayName("Guarda el aviso y devuelve el que persistió")
    void saveShouldReturnPersistedAviso() {
        when(jpa.saveAndFlush(any(AvisoEntity.class))).thenReturn(entity());

        Aviso result = repository.save(Aviso.builder().negocioId(NEGOCIO_ID).build());

        assertThat(result.getId()).isEqualTo(AvisoId.de(ID));
    }

    @Test
    @DisplayName("Busca el aviso por id dentro del negocio")
    void findByIdAndNegocioIdShouldMap() {
        when(jpa.findByIdAndNegocioId(ID, NEGOCIO_ID.valor())).thenReturn(Optional.of(entity()));

        assertThat(repository.findByIdAndNegocioId(AvisoId.de(ID), NEGOCIO_ID)).map(Aviso::getId)
                .contains(AvisoId.de(ID));
    }

    @Test
    @DisplayName("Busca el aviso por huella dentro del negocio")
    void findByNegocioIdAndHuellaShouldMap() {
        when(jpa.findByNegocioIdAndHuella(NEGOCIO_ID.valor(), "h")).thenReturn(Optional.of(entity()));

        assertThat(repository.findByNegocioIdAndHuella(NEGOCIO_ID, "h")).map(Aviso::getId).contains(AvisoId.de(ID));
    }

    @Test
    @DisplayName("Lista los avisos de un tipo y estado")
    void findByNegocioIdAndTipoAndEstadoShouldMap() {
        when(jpa.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID.valor(), TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of(entity()));

        assertThat(repository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .extracting(Aviso::getId).containsExactly(AvisoId.de(ID));
    }

    @Test
    @DisplayName("Lista todos los avisos del negocio")
    void findByNegocioIdShouldMap() {
        when(jpa.findByNegocioId(NEGOCIO_ID.valor())).thenReturn(List.of(entity()));

        assertThat(repository.findByNegocioId(NEGOCIO_ID)).extracting(Aviso::getId).containsExactly(AvisoId.de(ID));
    }

    @Test
    @DisplayName("Lista los avisos del negocio en un estado")
    void findByNegocioIdAndEstadoShouldMap() {
        when(jpa.findByNegocioIdAndEstado(NEGOCIO_ID.valor(), EstadoAviso.PENDIENTE)).thenReturn(List.of(entity()));

        assertThat(repository.findByNegocioIdAndEstado(NEGOCIO_ID, EstadoAviso.PENDIENTE))
                .extracting(Aviso::getId).containsExactly(AvisoId.de(ID));
    }

    private static AvisoEntity entity() {
        return AvisoEntity.builder().id(ID).negocioId(NEGOCIO_ID.valor()).tipo(TipoAviso.STOCK).grupo(GrupoAviso.STOCK)
                .estado(EstadoAviso.PENDIENTE).build();
    }
}
