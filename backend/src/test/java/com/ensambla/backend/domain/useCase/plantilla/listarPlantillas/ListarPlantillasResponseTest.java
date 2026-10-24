package com.ensambla.backend.domain.useCase.plantilla.listarPlantillas;

import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ListarPlantillasResponseTest {

    private static final PlantillaId PRIMER_ID = PlantillaId.nuevo();
    private static final PlantillaId SEGUNDO_ID = PlantillaId.nuevo();
    private static final String NOMBRE = "Inventario";
    private static final String DESCRIPCION = "Controlá tu stock";
    private static final String ICONO = "box";
    private static final Map<String, Object> ESTRUCTURA = Map.of("bloques", List.of());

    @Test
    @DisplayName("Mapea cada campo de la plantilla al item")
    void deShouldMapEveryPlantillaField() {
        ListarPlantillasResponse response = ListarPlantillasResponse.de(List.of(plantilla(PRIMER_ID)));

        thenItemHasEveryPlantillaField(response.content().getFirst());
    }

    @Test
    @DisplayName("Conserva el orden de las plantillas")
    void deShouldKeepPlantillasOrder() {
        ListarPlantillasResponse response =
                ListarPlantillasResponse.de(List.of(plantilla(PRIMER_ID), plantilla(SEGUNDO_ID)));

        assertThat(response.content()).extracting(PlantillaItem::id).containsExactly(PRIMER_ID, SEGUNDO_ID);
    }

    @Test
    @DisplayName("Devuelve contenido vacío cuando no hay plantillas")
    void deShouldReturnEmptyContentWhenThereAreNoPlantillas() {
        ListarPlantillasResponse response = ListarPlantillasResponse.de(List.of());

        assertThat(response.content()).isEmpty();
    }

    // --- helpers ---
    private Plantilla plantilla(PlantillaId id) {
        return Plantilla.builder()
                .id(id)
                .nombre(NOMBRE)
                .descripcion(DESCRIPCION)
                .icono(ICONO)
                .estructura(ESTRUCTURA)
                .build();
    }

    // --- assert ---
    private void thenItemHasEveryPlantillaField(PlantillaItem item) {
        assertThat(item.id()).isEqualTo(PRIMER_ID);
        assertThat(item.nombre()).isEqualTo(NOMBRE);
        assertThat(item.descripcion()).isEqualTo(DESCRIPCION);
        assertThat(item.icono()).isEqualTo(ICONO);
        assertThat(item.estructura()).isEqualTo(ESTRUCTURA);
    }
}
