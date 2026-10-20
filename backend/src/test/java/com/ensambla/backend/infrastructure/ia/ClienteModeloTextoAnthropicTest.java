package com.ensambla.backend.infrastructure.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.ensambla.backend.infrastructure.ia.RespuestasAnthropicDePrueba.anthropicQueResponde;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class ClienteModeloTextoAnthropicTest {

    private static final String MODELO = "claude-haiku-4-5";
    private static final PeticionModeloTexto PETICION = new PeticionModeloTexto("Sos Sami", "vendo velas", "{schema}");

    @Test
    @DisplayName("Devuelve el JSON sin el texto que lo rodea, con el modelo y los tokens")
    void completarShouldReturnJsonWithTokens() {
        AnthropicClient anthropic = anthropicQueResponde("Acá va: {\"plantilla\":\"Ventas\"} listo");

        RespuestaModeloTexto result = new ClienteModeloTextoAnthropic(anthropic, MODELO, 700).completar(PETICION);

        assertThat(result.json()).isEqualTo("{\"plantilla\":\"Ventas\"}");
        assertThat(result.proveedor()).isEqualTo("anthropic");
        assertThat(result.modelo()).isEqualTo(MODELO);
        assertThat(result.tokensEntrada()).isEqualTo(RespuestasAnthropicDePrueba.TOKENS_ENTRADA);
        assertThat(result.tokensSalida()).isEqualTo(RespuestasAnthropicDePrueba.TOKENS_SALIDA);
    }

    @Test
    @DisplayName("Manda el modelo, las instrucciones con el esquema, el contexto, los tokens y temperatura 0")
    void completarShouldSendInstructionsWithSchemaAndContext() {
        AnthropicClient anthropic = anthropicQueResponde("{}");

        new ClienteModeloTextoAnthropic(anthropic, MODELO, 700).completar(PETICION);

        MessageCreateParams pedido = pedidoEnviado(anthropic);
        assertThat(pedido.model().asString()).isEqualTo(MODELO);
        assertThat(pedido.maxTokens()).isEqualTo(700);
        assertThat(pedido.temperature()).contains(0.0);
        assertThat(pedido.system().orElseThrow().asString()).startsWith("Sos Sami").contains("{schema}");
        assertThat(pedido.messages().getFirst().content().asString()).isEqualTo("vendo velas");
    }

    // --- assert ---
    private MessageCreateParams pedidoEnviado(AnthropicClient anthropic) {
        ArgumentCaptor<MessageCreateParams> captor = ArgumentCaptor.forClass(MessageCreateParams.class);
        verify(anthropic.messages()).create(captor.capture());
        return captor.getValue();
    }
}
