package com.ensambla.backend.domain.mapper.sami;

import com.ensambla.backend.domain.dto.sami.ConversarEnTallerResponse;
import com.ensambla.backend.domain.dto.sami.EstadoTallerItem;
import com.ensambla.backend.domain.dto.sami.EstadoTallerItem.CampoTallerItem;
import com.ensambla.backend.domain.dto.sami.EstadoTallerItem.PiezaTallerItem;
import com.ensambla.backend.domain.dto.sami.ImagenTallerItem;
import com.ensambla.backend.domain.dto.sami.MensajeTallerItem;
import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.OpcionesTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversarEnTallerMapperTest {

    private final ConversarEnTallerMapper mapper = new ConversarEnTallerMapper();

    @Test
    @DisplayName("Convierte el estado del DTO al del dominio con sus piezas y campos")
    void toEstadoShouldMapPiecesAndFields() {
        EstadoTallerItem item = new EstadoTallerItem("Mi almacén", "mosaic", List.of(new PiezaTallerItem(
                "e1", "inventory", "Inventario", "General", List.of(new CampoTallerItem("Producto", "text")))));

        EstadoTaller result = mapper.toEstado(item);

        assertThat(result).isEqualTo(new EstadoTaller("Mi almacén", "mosaic", List.of(new PiezaTaller(
                "e1", "inventory", "Inventario", "General", List.of(new CampoTaller("Producto", "text"))))));
    }

    @Test
    @DisplayName("Convierte el historial de mensajes")
    void toHistorialShouldMapMessages() {
        List<MensajeTaller> result = mapper.toHistorial(List.of(new MensajeTallerItem("sami", "Hola")));

        assertThat(result).containsExactly(new MensajeTaller("sami", "Hola"));
    }

    @Test
    @DisplayName("Arma la respuesta con el texto, las acciones y la botonera")
    void toResponseShouldMapTextActionsAndOptions() {
        RespuestaTaller respuesta = new RespuestaTaller(
                "Listo",
                List.of(new AccionTaller("agregar_pieza", Map.of("tipo", "expenses"))),
                new OpcionesTaller("¿Seguimos?", List.of("Sí", "No")));

        ConversarEnTallerResponse result = mapper.toResponse(respuesta);

        assertThat(result.respuesta()).isEqualTo("Listo");
        assertThat(result.acciones()).extracting("herramienta").containsExactly("agregar_pieza");
        assertThat(result.acciones().getFirst().argumentos()).containsEntry("tipo", "expenses");
        assertThat(result.opciones().pregunta()).isEqualTo("¿Seguimos?");
        assertThat(result.opciones().opciones()).containsExactly("Sí", "No");
    }

    @Test
    @DisplayName("Sin botonera la respuesta no lleva opciones")
    void toResponseShouldLeaveOptionsNullWhenThereAreNone() {
        ConversarEnTallerResponse result = mapper.toResponse(new RespuestaTaller("Hola", List.of(), null));

        assertThat(result.opciones()).isNull();
        assertThat(result.acciones()).isEmpty();
    }

    @Test
    @DisplayName("Convierte la foto adjunta con su tipo y sus bytes")
    void toImagenShouldMapTypeAndBytes() {
        ImagenTaller result = mapper.toImagen(new ImagenTallerItem("image/webp", new byte[]{7, 8}));

        assertThat(result.tipo()).isEqualTo("image/webp");
        assertThat(result.datos()).containsExactly(7, 8);
    }

    @Test
    @DisplayName("Sin foto adjunta no hay imagen")
    void toImagenShouldReturnNullWithoutImage() {
        assertThat(mapper.toImagen(null)).isNull();
    }
}
