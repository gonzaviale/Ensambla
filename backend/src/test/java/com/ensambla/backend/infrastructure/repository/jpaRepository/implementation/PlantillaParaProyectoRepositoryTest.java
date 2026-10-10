package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.infrastructure.repository.entity.PlantillaEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPlantillaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PlantillaRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.PLANTILLA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlantillaParaProyectoRepositoryTest {

    private final IPlantillaJpaRepository jpa = mock(IPlantillaJpaRepository.class);
    private final PlantillaRepositoryImpl repository = new PlantillaRepositoryImpl(jpa, new PlantillaRepositoryMapper());
    private final PlantillaService plantillaService = new PlantillaService(repository);

    @Test
    @DisplayName("Convierte una plantilla activa a estructura tipada con valores opcionales por defecto")
    void obtenerParaProyectoShouldReadTypedStructure() {
        when(jpa.findByIdAndActivaTrue(PLANTILLA_ID)).thenReturn(Optional.of(PlantillaEntity.builder()
                .id(PLANTILLA_ID)
                .nombre("Inventario")
                .descripcion("Mis productos")
                .estructura(Map.of("bloques", List.of(Map.of("nombre", "Productos", "campos", List.of(
                        Map.of("nombre", "Nombre", "tipo", "TEXTO"))))))
                .build()));

        var result = plantillaService.obtenerParaProyecto(PLANTILLA_ID);

        assertThat(result.id()).isEqualTo(PLANTILLA_ID);
        assertThat(result.nombre()).isEqualTo("Inventario");
        assertThat(result.descripcion()).isEqualTo("Mis productos");
        var campo = result.estructura().bloques().getFirst().campos().getFirst();
        assertThat(campo.obligatorio()).isFalse();
        assertThat(campo.opciones()).isEmpty();
    }

    @Test
    @DisplayName("Una plantilla inactiva o inexistente responde no encontrada")
    void obtenerParaProyectoShouldRejectInactiveOrMissing() {
        when(jpa.findByIdAndActivaTrue(PLANTILLA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> plantillaService.obtenerParaProyecto(PLANTILLA_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

