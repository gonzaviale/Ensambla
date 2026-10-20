package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.enums.EstadoAviso;
import com.ensambla.backend.domain.model.enums.GrupoAviso;
import com.ensambla.backend.domain.model.enums.TipoAviso;
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
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");

    private final IAvisoJpaRepository jpa = mock(IAvisoJpaRepository.class);
    private final AvisoRepositoryImpl repository = new AvisoRepositoryImpl(jpa, new AvisoRepositoryMapper());

    @Test
    @DisplayName("Guarda el aviso y devuelve el que persistió")
    void saveShouldReturnPersistedAviso() {
        when(jpa.saveAndFlush(any(AvisoEntity.class))).thenReturn(entity());

        Aviso result = repository.save(Aviso.builder().negocioId(NEGOCIO_ID).build());

        assertThat(result.getId()).isEqualTo(ID);
    }

    @Test
    @DisplayName("Busca el aviso por id dentro del negocio")
    void findByIdAndNegocioIdShouldMap() {
        when(jpa.findByIdAndNegocioId(ID, NEGOCIO_ID)).thenReturn(Optional.of(entity()));

        assertThat(repository.findByIdAndNegocioId(ID, NEGOCIO_ID)).map(Aviso::getId).contains(ID);
    }

    @Test
    @DisplayName("Busca el aviso por huella dentro del negocio")
    void findByNegocioIdAndHuellaShouldMap() {
        when(jpa.findByNegocioIdAndHuella(NEGOCIO_ID, "h")).thenReturn(Optional.of(entity()));

        assertThat(repository.findByNegocioIdAndHuella(NEGOCIO_ID, "h")).map(Aviso::getId).contains(ID);
    }

    @Test
    @DisplayName("Lista los avisos de un tipo y estado")
    void findByNegocioIdAndTipoAndEstadoShouldMap() {
        when(jpa.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of(entity()));

        assertThat(repository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .extracting(Aviso::getId).containsExactly(ID);
    }

    @Test
    @DisplayName("Lista todos los avisos del negocio")
    void findByNegocioIdShouldMap() {
        when(jpa.findByNegocioId(NEGOCIO_ID)).thenReturn(List.of(entity()));

        assertThat(repository.findByNegocioId(NEGOCIO_ID)).extracting(Aviso::getId).containsExactly(ID);
    }

    @Test
    @DisplayName("Lista los avisos del negocio en un estado")
    void findByNegocioIdAndEstadoShouldMap() {
        when(jpa.findByNegocioIdAndEstado(NEGOCIO_ID, EstadoAviso.PENDIENTE)).thenReturn(List.of(entity()));

        assertThat(repository.findByNegocioIdAndEstado(NEGOCIO_ID, EstadoAviso.PENDIENTE))
                .extracting(Aviso::getId).containsExactly(ID);
    }

    private static AvisoEntity entity() {
        return AvisoEntity.builder().id(ID).negocioId(NEGOCIO_ID).tipo(TipoAviso.STOCK).grupo(GrupoAviso.STOCK)
                .estado(EstadoAviso.PENDIENTE).build();
    }
}
