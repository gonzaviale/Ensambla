package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptTallerTest {

    private final PromptTaller prompt = new PromptTaller(new ClassPathResource("ia/prompt-taller.md"));

    @Test
    @DisplayName("El contexto lista las piezas con su id, tipo, sección y datos, y termina con el mensaje")
    void peticionShouldListPiecesAndEndWithTheMessage() {
        EstadoTaller estado = new EstadoTaller("Mi almacén", "mosaic", List.of(
                new PiezaTaller("e1", "inventory", "Inventario", null,
                        List.of(new CampoTaller("Producto", "text"), new CampoTaller("Cantidad", "number"))),
                new PiezaTaller("e2", "clients", "Clientes", "Aparte", List.of())));

        PeticionConHerramientas peticion = prompt.peticion(new ConsultaTaller("sumá gastos", estado, List.of()));

        assertThat(peticion.contexto())
                .startsWith("Sistema actual: «Mi almacén» (distribución: mosaic)\n")
                .contains("- id e1, tipo inventory, «Inventario»: Producto (text), Cantidad (number)\n")
                .contains("- id e2, tipo clients, «Clientes» sección «Aparte»: \n")
                .endsWith("Mensaje de la persona: sumá gastos")
                .doesNotContain("Conversación reciente");
        assertThat(peticion.instrucciones()).contains("Sos Sami").contains("ofrecer_opciones");
    }

    @Test
    @DisplayName("Un sistema sin piezas lo dice")
    void peticionShouldSayWhenThereAreNoPieces() {
        PeticionConHerramientas peticion = prompt.peticion(
                new ConsultaTaller("hola", new EstadoTaller("", "mosaic", List.of()), List.of()));

        assertThat(peticion.contexto()).contains("Todavía no tiene piezas.");
    }

    @Test
    @DisplayName("La charla reciente va con quién dijo cada cosa")
    void peticionShouldIncludeRecentConversation() {
        PeticionConHerramientas peticion = prompt.peticion(new ConsultaTaller(
                "dale", new EstadoTaller("S", "mosaic", List.of()),
                List.of(new MensajeTaller("sami", "¿Sumamos gastos?"), new MensajeTaller("you", "tal vez"))));

        assertThat(peticion.contexto()).contains("Conversación reciente:\nSami: ¿Sumamos gastos?\nPersona: tal vez\n");
    }

    @Test
    @DisplayName("Si falta el recurso del prompt la aplicación no levanta")
    void constructorShouldFailWhenResourceIsMissing() {
        assertThatThrownBy(() -> new PromptTaller(new ClassPathResource("ia/no-existe.md")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(new PromptTaller(new ByteArrayResource("Sos Sami".getBytes())).peticion(
                new ConsultaTaller("x", new EstadoTaller("", "mosaic", List.of()), List.of())).instrucciones())
                .isEqualTo("Sos Sami");
    }
}
