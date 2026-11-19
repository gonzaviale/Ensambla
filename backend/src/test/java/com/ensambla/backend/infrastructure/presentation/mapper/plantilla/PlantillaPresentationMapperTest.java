package com.ensambla.backend.infrastructure.presentation.mapper.plantilla;

import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.useCase.plantilla.listarPlantillas.ListarPlantillasResponse;
import com.ensambla.backend.domain.useCase.plantilla.listarPlantillas.PlantillaItem;
import com.ensambla.backend.infrastructure.presentation.dto.plantilla.PlantillaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlantillaPresentationMapperTest {

    private static final PlantillaId PLANTILLA_ID = PlantillaId.nuevo();
    private static final Map<String, Object> ESTRUCTURA = Map.of("bloques", List.of());

    private final PlantillaPresentationMapper mapper = new PlantillaPresentationMapper();
    private List<PlantillaResponse> plantillas;

    @Test
    @DisplayName("Mapea cada campo del item a la respuesta")
    void mapeaCadaCampoDelItemALaRespuesta() {
        cuandoSeRespondenLasPlantillas(new PlantillaItem(PLANTILLA_ID, "Inventario", "Controlá tu stock", "box", ESTRUCTURA));

        entoncesLaUnicaPlantillaEs(new PlantillaResponse(PLANTILLA_ID, "Inventario", "Controlá tu stock", "box", ESTRUCTURA));
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando no hay plantillas")
    void devuelveUnaListaVaciaSinPlantillas() {
        cuandoSeRespondenLasPlantillas();

        entoncesNoHayPlantillas();
    }

    private void cuandoSeRespondenLasPlantillas(PlantillaItem... items) {
        plantillas = mapper.toResponse(new ListarPlantillasResponse(List.of(items)));
    }

    private void entoncesLaUnicaPlantillaEs(PlantillaResponse esperada) {
        assertThat(plantillas).containsExactly(esperada);
    }

    private void entoncesNoHayPlantillas() {
        assertThat(plantillas).isEmpty();
    }
}