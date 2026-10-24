package com.ensambla.backend.infrastructure.presentation.mapper.plantilla;

import com.ensambla.backend.domain.useCase.plantilla.listarPlantillas.ListarPlantillasResponse;
import com.ensambla.backend.domain.useCase.plantilla.listarPlantillas.PlantillaItem;
import com.ensambla.backend.infrastructure.presentation.dto.plantilla.PlantillaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlantillaPresentationMapperTest {

    private static final UUID PLANTILLA_ID = UUID.randomUUID();
    private static final Map<String, Object> ESTRUCTURA = Map.of("bloques", List.of());

    private PlantillaPresentationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PlantillaPresentationMapper();
    }

    @Test
    @DisplayName("Mapea cada campo del item a la respuesta")
    void toResponseShouldMapEveryItemField() {
        ListarPlantillasResponse response = new ListarPlantillasResponse(List.of(
                new PlantillaItem(PLANTILLA_ID, "Inventario", "Controlá tu stock", "box", ESTRUCTURA)));

        List<PlantillaResponse> result = mapper.toResponse(response);

        assertThat(result).hasSize(1);
        PlantillaResponse plantilla = result.getFirst();
        assertThat(plantilla.id()).isEqualTo(PLANTILLA_ID);
        assertThat(plantilla.nombre()).isEqualTo("Inventario");
        assertThat(plantilla.descripcion()).isEqualTo("Controlá tu stock");
        assertThat(plantilla.icono()).isEqualTo("box");
        assertThat(plantilla.estructura()).isEqualTo(ESTRUCTURA);
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando no hay plantillas")
    void toResponseShouldReturnEmptyListWhenThereAreNoPlantillas() {
        List<PlantillaResponse> result = mapper.toResponse(new ListarPlantillasResponse(List.of()));

        assertThat(result).isEmpty();
    }
}
