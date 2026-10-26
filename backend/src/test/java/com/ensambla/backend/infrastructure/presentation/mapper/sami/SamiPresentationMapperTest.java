package com.ensambla.backend.infrastructure.presentation.mapper.sami;

import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.sami.ColumnaSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiRequest;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiResponse;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.OpcionSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.PreguntaSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.PropuestaSamiItem;
import com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami.CrearProyectoDesdeSamiRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarSamiRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarSamiResponse;
import com.ensambla.backend.infrastructure.presentation.dto.sami.CrearProyectoSamiRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.RespuestaSamiRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SamiPresentationMapperTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.nuevo();
    private static final PlantillaId PLANTILLA_ID = PlantillaId.nuevo();

    private final SamiPresentationMapper mapper = new SamiPresentationMapper();

    @Test
    @DisplayName("Arma el pedido con el usuario de la sesión, el texto sin espacios sobrantes y las respuestas")
    void toRequestShouldTrimTextAndMapAnswers() {
        ConversarSamiRequest body = new ConversarSamiRequest("  vendo velas  ",
                List.of(new RespuestaSamiRequest("principal", "ventas")));

        ConversarConSamiRequest result = mapper.toRequest(USUARIO_ID, body);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.texto()).isEqualTo("vendo velas");
        assertThat(result.respuestas()).extracting("preguntaId", "opcionId")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("principal", "ventas"));
    }

    @Test
    @DisplayName("Sin respuestas en el body, el pedido lleva una lista vacía")
    void toRequestShouldUseEmptyListWhenThereAreNoAnswers() {
        ConversarConSamiRequest result = mapper.toRequest(USUARIO_ID, new ConversarSamiRequest("hola", null));

        assertThat(result.respuestas()).isEmpty();
    }

    @Test
    @DisplayName("Arma el pedido de crear proyecto con el usuario de la sesión, la plantilla, el nombre y las columnas")
    void toCrearProyectoRequestShouldMapBodyWithSessionUser() {
        CrearProyectoSamiRequest body = new CrearProyectoSamiRequest(PLANTILLA_ID, "Velas", List.of(
                new CrearProyectoSamiRequest.ColumnaSamiRequest("Saldo pendiente", "NUMERO", null),
                new CrearProyectoSamiRequest.ColumnaSamiRequest("Forma de pago", "OPCION", List.of("A", "B"))));

        CrearProyectoDesdeSamiRequest result = mapper.toCrearProyectoRequest(USUARIO_ID, body);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.plantillaId()).isEqualTo(PLANTILLA_ID);
        assertThat(result.nombre()).isEqualTo("Velas");
        assertThat(result.columnas()).extracting("nombre", "tipo", "opciones").containsExactly(
                org.assertj.core.groups.Tuple.tuple("Saldo pendiente", "NUMERO", null),
                org.assertj.core.groups.Tuple.tuple("Forma de pago", "OPCION", List.of("A", "B")));
    }

    @Test
    @DisplayName("Mapea una respuesta con pregunta")
    void toResponseShouldMapQuestion() {
        ConversarConSamiResponse response = new ConversarConSamiResponse("PREGUNTA",
                new PreguntaSamiItem("principal", "¿Qué querés anotar?", List.of(new OpcionSamiItem("ventas", "Vender"))),
                null, 1);

        ConversarSamiResponse result = mapper.toResponse(response);

        assertThat(result.tipo()).isEqualTo("PREGUNTA");
        assertThat(result.pregunta().opciones()).extracting("id", "texto")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("ventas", "Vender"));
        assertThat(result.propuesta()).isNull();
        assertThat(result.preguntasHechas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Mapea una respuesta con propuesta")
    void toResponseShouldMapProposal() {
        ConversarConSamiResponse response = new ConversarConSamiResponse("PROPUESTA", null,
                new PropuestaSamiItem(PLANTILLA_ID, "Ventas",
                        List.of(new ColumnaSamiItem("Saldo pendiente", "NUMERO", List.of())), 0.9, "ALGORITMO"),
                0);

        ConversarSamiResponse result = mapper.toResponse(response);

        assertThat(result.pregunta()).isNull();
        assertThat(result.propuesta().plantillaId()).isEqualTo(PLANTILLA_ID);
        assertThat(result.propuesta().columnas()).extracting("nombre", "tipo")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Saldo pendiente", "NUMERO"));
        assertThat(result.propuesta().origen()).isEqualTo("ALGORITMO");
    }
}
