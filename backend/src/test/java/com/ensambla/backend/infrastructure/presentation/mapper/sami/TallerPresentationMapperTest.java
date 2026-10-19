package com.ensambla.backend.infrastructure.presentation.mapper.sami;

import com.ensambla.backend.domain.dto.sami.ConversarEnTallerRequest;
import com.ensambla.backend.domain.dto.sami.ConversarEnTallerResponse;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarTallerRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarTallerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TallerPresentationMapperTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();

    private final TallerPresentationMapper mapper = new TallerPresentationMapper();

    @Test
    @DisplayName("Arma el pedido con el usuario de la sesión, el mensaje sin espacios, el estado y el historial")
    void toRequestShouldMapBodyWithSessionUser() {
        ConversarTallerRequest body = new ConversarTallerRequest(
                "  sumá gastos  ",
                new ConversarTallerRequest.Estado("Mi almacén", "mosaic", List.of(new ConversarTallerRequest.Pieza(
                        "e1", "inventory", "Inventario", "General",
                        List.of(new ConversarTallerRequest.Campo("Producto", "text"))))),
                List.of(new ConversarTallerRequest.Mensaje("sami", "Hola")));

        ConversarEnTallerRequest result = mapper.toRequest(USUARIO_ID, body);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.mensaje()).isEqualTo("sumá gastos");
        assertThat(result.estado().nombre()).isEqualTo("Mi almacén");
        assertThat(result.estado().piezas().getFirst().campos()).extracting("etiqueta").containsExactly("Producto");
        assertThat(result.historial()).extracting("texto").containsExactly("Hola");
    }

    @Test
    @DisplayName("Sin historial en el body, el pedido lleva una lista vacía")
    void toRequestShouldUseEmptyHistoryWhenMissing() {
        ConversarTallerRequest body = new ConversarTallerRequest(
                "hola", new ConversarTallerRequest.Estado("", "mosaic", List.of()), null);

        assertThat(mapper.toRequest(USUARIO_ID, body).historial()).isEmpty();
    }

    @Test
    @DisplayName("Mapea la respuesta con acciones y botonera")
    void toResponseShouldMapActionsAndOptions() {
        ConversarEnTallerResponse response = new ConversarEnTallerResponse(
                "Listo",
                List.of(new ConversarEnTallerResponse.AccionTallerItem("agregar_pieza", Map.of("tipo", "expenses"))),
                new ConversarEnTallerResponse.OpcionesTallerItem("¿Seguimos?", List.of("Sí", "No")));

        ConversarTallerResponse result = mapper.toResponse(response);

        assertThat(result.respuesta()).isEqualTo("Listo");
        assertThat(result.acciones().getFirst().herramienta()).isEqualTo("agregar_pieza");
        assertThat(result.acciones().getFirst().argumentos()).containsEntry("tipo", "expenses");
        assertThat(result.opciones().opciones()).containsExactly("Sí", "No");
    }

    @Test
    @DisplayName("Sin botonera la respuesta no lleva opciones")
    void toResponseShouldLeaveOptionsNullWhenThereAreNone() {
        ConversarTallerResponse result = mapper.toResponse(new ConversarEnTallerResponse("Hola", List.of(), null));

        assertThat(result.opciones()).isNull();
    }
}
