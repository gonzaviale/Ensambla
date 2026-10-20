package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.infrastructure.repository.entity.ZonaEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IZonaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ZonaRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZonaRepositoryImplTest {

    private final IZonaJpaRepository zonaJpaRepository = mock(IZonaJpaRepository.class);
    private final ZonaRepositoryImpl zonaRepository = new ZonaRepositoryImpl(
            zonaJpaRepository, new ZonaRepositoryMapper());

    @Test
    @DisplayName("Lista todas las zonas como dominio")
    void findAllShouldMapEveryZone() {
        UUID id = UUID.randomUUID();
        when(zonaJpaRepository.findAll()).thenReturn(List.of(ZonaEntity.builder().id(id).nombre("San Justo").build()));

        List<Zona> result = zonaRepository.findAll();

        assertThat(result).extracting(Zona::getId).containsExactly(id);
    }

    @Test
    @DisplayName("Busca una zona por id")
    void findByIdShouldMapZone() {
        UUID id = UUID.randomUUID();
        when(zonaJpaRepository.findById(id)).thenReturn(Optional.of(ZonaEntity.builder().id(id).nombre("San Justo").build()));

        Optional<Zona> result = zonaRepository.findById(id);

        assertThat(result).map(Zona::getNombre).contains("San Justo");
    }
}
