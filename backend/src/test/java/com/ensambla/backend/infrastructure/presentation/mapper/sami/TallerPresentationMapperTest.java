package com.ensambla.backend.infrastructure.presentation.mapper.sami;

import com.ensambla.backend.domain.dto.sami.ConversarEnTallerRequest;
import com.ensambla.backend.domain.dto.sami.ConversarEnTallerResponse;
import com.ensambla.backend.domain.dto.sami.ImagenTallerItem;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarTallerRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarTallerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                        List.of(new ConversarTallerRequest.Campo("Producto", "text")),
                        List.of(new ConversarTallerRequest.Fila("f1", List.of("Yerba")))))),
                List.of(new ConversarTallerRequest.Mensaje("sami", "Hola")), null);

        ConversarEnTallerRequest result = mapper.toRequest(USUARIO_ID, body);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.mensaje()).isEqualTo("sumá gastos");
        assertThat(result.estado().nombre()).isEqualTo("Mi almacén");
        assertThat(result.estado().piezas().getFirst().campos()).extracting("etiqueta").containsExactly("Producto");
        assertThat(result.estado().piezas().getFirst().filas()).extracting("id").containsExactly("f1");
        assertThat(result.historial()).extracting("texto").containsExactly("Hola");
        assertThat(result.imagen()).isNull();
    }

    @Test
    @DisplayName("Sin historial en el body, el pedido lleva una lista vacía")
    void toRequestShouldUseEmptyHistoryWhenMissing() {
        ConversarTallerRequest body = new ConversarTallerRequest(
                "hola", new ConversarTallerRequest.Estado("", "mosaic", List.of()), null, null);

        assertThat(mapper.toRequest(USUARIO_ID, body).historial()).isEmpty();
    }

    @Test
    @DisplayName("La foto adjunta llega decodificada, con su tipo")
    void toRequestShouldDecodeAttachedImage() {
        ConversarTallerRequest body = new ConversarTallerRequest("te paso la foto",
                new ConversarTallerRequest.Estado("", "mosaic", List.of()), null,
                new ConversarTallerRequest.Imagen("image/png", "AQID"));

        ImagenTallerItem result = mapper.toRequest(USUARIO_ID, body).imagen();

        assertThat(result.tipo()).isEqualTo("image/png");
        assertThat(result.datos()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("Una foto de un tipo que no va se rechaza con un mensaje para la persona")
    void toRequestShouldRejectInvalidImageType() {
        ConversarTallerRequest body = new ConversarTallerRequest("te paso la foto",
                new ConversarTallerRequest.Estado("", "mosaic", List.of()), null,
                new ConversarTallerRequest.Imagen("image/gif", "AQID"));

        assertThatThrownBy(() -> mapper.toRequest(USUARIO_ID, body))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(ImagenTaller.TIPO_INVALIDO);
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
