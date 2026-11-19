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

    private ListarPlantillasResponse respuesta;

    @Test
    @DisplayName("Mapea cada campo de la plantilla al item")
    void mapeaCadaCampoDeLaPlantillaAlItem() {
        cuandoSeArmaLaRespuestaConLasPlantillas(unaPlantilla(PRIMER_ID));

        entoncesElItemTieneTodosLosCamposDeLaPlantilla();
    }

    @Test
    @DisplayName("Conserva el orden de las plantillas")
    void conservaElOrdenDeLasPlantillas() {
        cuandoSeArmaLaRespuestaConLasPlantillas(unaPlantilla(PRIMER_ID), unaPlantilla(SEGUNDO_ID));

        entoncesLasPlantillasQuedanEnElOrdenOriginal();
    }

    @Test
    @DisplayName("Devuelve contenido vacío cuando no hay plantillas")
    void devuelveContenidoVacioCuandoNoHayPlantillas() {
        cuandoSeArmaLaRespuestaConLasPlantillas();

        entoncesElContenidoEstaVacio();
    }

    // --- cuando ---

    private void cuandoSeArmaLaRespuestaConLasPlantillas(Plantilla... plantillas) {
        respuesta = ListarPlantillasResponse.de(List.of(plantillas));
    }

    // --- entonces ---

    private void entoncesElItemTieneTodosLosCamposDeLaPlantilla() {
        PlantillaItem item = respuesta.content().getFirst();
        assertThat(item.id()).isEqualTo(PRIMER_ID);
        assertThat(item.nombre()).isEqualTo(NOMBRE);
        assertThat(item.descripcion()).isEqualTo(DESCRIPCION);
        assertThat(item.icono()).isEqualTo(ICONO);
        assertThat(item.estructura()).isEqualTo(ESTRUCTURA);
    }

    private void entoncesLasPlantillasQuedanEnElOrdenOriginal() {
        assertThat(respuesta.content()).extracting(PlantillaItem::id).containsExactly(PRIMER_ID, SEGUNDO_ID);
    }

    private void entoncesElContenidoEstaVacio() {
        assertThat(respuesta.content()).isEmpty();
    }

    // --- datos ---

    private Plantilla unaPlantilla(PlantillaId id) {
        return Plantilla.builder()
                .id(id)
                .nombre(NOMBRE)
                .descripcion(DESCRIPCION)
                .icono(ICONO)
                .estructura(ESTRUCTURA)
                .build();
    }
}
