package com.ensambla.backend.infrastructure.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteModeloConHerramientasSimuladoTest {

    private final ClienteModeloConHerramientasSimulado cliente = new ClienteModeloConHerramientasSimulado();

    @Test
    @DisplayName("Si la persona nombra gastos, suma la pieza de gastos")
    void completarShouldAddExpensesWhenMessageMentionsThem() {
        RespuestaConHerramientas result = cliente.completar(contextoCon("Mensaje de la persona: sumá los gastos"));

        assertThat(result.llamadas()).hasSize(1);
        assertThat(result.llamadas().getFirst().nombre()).isEqualTo("agregar_pieza");
        assertThat(result.llamadas().getFirst().argumentos()).containsEntry("tipo", "expenses");
        assertThat(result.proveedor()).isEqualTo("simulado");
        assertThat(result.tokensEntrada()).isZero();
    }

    @Test
    @DisplayName("En cualquier otro caso pregunta con una botonera")
    void completarShouldAskWithOptionsOtherwise() {
        RespuestaConHerramientas result = cliente.completar(contextoCon("Mensaje de la persona: ayuda"));

        assertThat(result.llamadas().getFirst().nombre()).isEqualTo("ofrecer_opciones");
        assertThat(result.llamadas().getFirst().argumentos()).containsKeys("pregunta", "opciones");
    }

    @Test
    @DisplayName("Sólo mira el mensaje de la persona, no el resto del contexto")
    void completarShouldIgnoreTheRestOfTheContext() {
        String contexto = "- id e3, tipo expenses, «Gastos»: Detalle (text)\n\nMensaje de la persona: hola";

        RespuestaConHerramientas result = cliente.completar(contextoCon(contexto));

        assertThat(result.llamadas().getFirst().nombre()).isEqualTo("ofrecer_opciones");
    }

    @Test
    @DisplayName("Sin la marca del mensaje mira todo el contexto")
    void completarShouldReadTheWholeContextWhenThereIsNoMarker() {
        RespuestaConHerramientas result = cliente.completar(contextoCon("quiero gastos"));

        assertThat(result.llamadas().getFirst().nombre()).isEqualTo("agregar_pieza");
    }

    private PeticionConHerramientas contextoCon(String contexto) {
        return new PeticionConHerramientas("instrucciones", contexto);
    }
}
