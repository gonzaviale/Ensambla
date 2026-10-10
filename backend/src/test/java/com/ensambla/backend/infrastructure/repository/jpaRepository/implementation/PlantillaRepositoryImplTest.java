package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.infrastructure.repository.entity.PlantillaEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPlantillaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PlantillaRepositoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlantillaRepositoryImplTest {

    private static final UUID PRIMER_ID = UUID.randomUUID();
    private static final UUID SEGUNDO_ID = UUID.randomUUID();

    @Mock
    private IPlantillaJpaRepository jpa;

    private PlantillaRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new PlantillaRepositoryImpl(jpa, new PlantillaRepositoryMapper());
    }

    @Test
    @DisplayName("Devuelve las plantillas activas mapeadas y en el orden de la consulta")
    void findAllActivasShouldReturnMappedPlantillasInOrder() {
        when(jpa.findByActivaTrueOrderByNombreAscIdAsc()).thenReturn(List.of(
                PlantillaEntity.builder().id(PRIMER_ID).nombre("Clientes").build(),
                PlantillaEntity.builder().id(SEGUNDO_ID).nombre("Pedidos").build()));

        List<Plantilla> result = repository.findAllActivas();

        assertThat(result).extracting(Plantilla::getId).containsExactly(PRIMER_ID, SEGUNDO_ID);
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando no hay plantillas activas")
    void findAllActivasShouldReturnEmptyListWhenThereAreNone() {
        when(jpa.findByActivaTrueOrderByNombreAscIdAsc()).thenReturn(List.of());

        assertThat(repository.findAllActivas()).isEmpty();
    }
}
