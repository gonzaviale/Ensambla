package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.OpcionesTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem.CampoTallerItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem.PiezaTallerItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversarEnTallerDtosTest {

    @Test
    @DisplayName("Convierte el estado del DTO al del dominio con sus piezas y campos")
    void deShouldMapPiecesAndFields() {
        EstadoTallerItem item = new EstadoTallerItem("Mi almacén", "mosaic", List.of(new PiezaTallerItem(
                "e1", "inventory", "Inventario", "General", List.of(new CampoTallerItem("Producto", "text")))));

        EstadoTaller result = item.aEstadoTaller();

        assertThat(result).isEqualTo(new EstadoTaller("Mi almacén", "mosaic", List.of(new PiezaTaller(
                "e1", "inventory", "Inventario", "General", List.of(new CampoTaller("Producto", "text"))))));
    }

    @Test
    @DisplayName("Convierte el historial de mensajes")
    void deShouldMapMessages() {
        List<MensajeTaller> result = conHistorial(List.of(new MensajeTallerItem("sami", "Hola"))).historialTaller();

        assertThat(result).containsExactly(new MensajeTaller("sami", "Hola"));
    }

    @Test
    @DisplayName("Arma la respuesta con el texto, las acciones y la botonera")
    void deShouldMapTextActionsAndOptions() {
        RespuestaTaller respuesta = new RespuestaTaller(
                "Listo",
                List.of(new AccionTaller("agregar_pieza", Map.of("tipo", "expenses"))),
                List.of(new OpcionesTaller("Seguir", "¿Seguimos?", List.of("Sí", "No"))));

        ConversarEnTallerResponse result = ConversarEnTallerResponse.de(respuesta);

        assertThat(result.respuesta()).isEqualTo("Listo");
        assertThat(result.acciones()).extracting("herramienta").containsExactly("agregar_pieza");
        assertThat(result.acciones().getFirst().argumentos()).containsEntry("tipo", "expenses");
        assertThat(result.preguntas()).hasSize(1);
        assertThat(result.preguntas().getFirst().tema()).isEqualTo("Seguir");
        assertThat(result.preguntas().getFirst().pregunta()).isEqualTo("¿Seguimos?");
        assertThat(result.preguntas().getFirst().opciones()).containsExactly("Sí", "No");
    }

    @Test
    @DisplayName("Sin botonera la respuesta no lleva opciones")
    void deShouldLeaveOptionsNullWhenThereAreNone() {
        ConversarEnTallerResponse result =
                ConversarEnTallerResponse.de(new RespuestaTaller("Hola", List.of(), List.of()));

        assertThat(result.preguntas()).isEmpty();
        assertThat(result.acciones()).isEmpty();
    }

    @Test
    @DisplayName("Convierte la foto adjunta con su tipo y sus bytes")
    void deShouldMapTypeAndBytes() {
        ImagenTaller result = conImagen(new ImagenTallerItem("image/webp", new byte[]{7, 8})).imagenTaller();

        assertThat(result.tipo()).isEqualTo("image/webp");
        assertThat(result.datos()).containsExactly(7, 8);
    }

    @Test
    @DisplayName("Sin foto adjunta no hay imagen")
    void deShouldReturnNullWithoutImage() {
        assertThat(conImagen(null).imagenTaller()).isNull();
    }

    // --- helpers ---
    private static ConversarEnTallerRequest conImagen(ImagenTallerItem imagen) {
        return new ConversarEnTallerRequest(null, null, null, List.of(), imagen);
    }

    private static ConversarEnTallerRequest conHistorial(List<MensajeTallerItem> historial) {
        return new ConversarEnTallerRequest(null, null, null, historial, null);
    }
}
