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

    private AnthropicClient anthropic;
    private RespuestaModeloTexto respuesta;

    @Test
    @DisplayName("Devuelve el JSON sin el texto que lo rodea, con el modelo y los tokens")
    void devuelveElJsonSinElTextoQueLoRodeaConElModeloYLosTokens() {
        dadoUnAnthropicQueResponde("Acá va: {\"plantilla\":\"Ventas\"} listo");

        cuandoSeCompleta();

        entoncesDevuelveElJsonConElModeloYLosTokens();
    }

    @Test
    @DisplayName("Manda el modelo, las instrucciones con el esquema, el contexto y los tokens, sin temperatura")
    void mandaElModeloLasInstruccionesConElEsquemaElContextoYLosTokensSinTemperatura() {
        dadoUnAnthropicQueResponde("{}");

        cuandoSeCompleta();

        entoncesElPedidoLlevaModeloInstruccionesConEsquemaYContexto();
    }

    // --- dado ---

    private void dadoUnAnthropicQueResponde(String texto) {
        anthropic = anthropicQueResponde(texto);
    }

    // --- cuando ---

    private void cuandoSeCompleta() {
        respuesta = new ClienteModeloTextoAnthropic(anthropic, MODELO, 700).completar(PETICION);
    }

    // --- entonces ---

    private void entoncesDevuelveElJsonConElModeloYLosTokens() {
        assertThat(respuesta.json()).isEqualTo("{\"plantilla\":\"Ventas\"}");
        assertThat(respuesta.proveedor()).isEqualTo("anthropic");
        assertThat(respuesta.modelo()).isEqualTo(MODELO);
        assertThat(respuesta.tokensEntrada()).isEqualTo(RespuestasAnthropicDePrueba.TOKENS_ENTRADA);
        assertThat(respuesta.tokensSalida()).isEqualTo(RespuestasAnthropicDePrueba.TOKENS_SALIDA);
    }

    private void entoncesElPedidoLlevaModeloInstruccionesConEsquemaYContexto() {
        MessageCreateParams pedido = pedidoEnviado();
        assertThat(pedido.model().asString()).isEqualTo(MODELO);
        assertThat(pedido.maxTokens()).isEqualTo(700);
        assertThat(pedido.temperature()).isEmpty();
        assertThat(pedido.system().orElseThrow().asString()).startsWith("Sos Sami").contains("{schema}");
        assertThat(pedido.messages().getFirst().content().asString()).isEqualTo("vendo velas");
    }

    // --- datos ---

    private MessageCreateParams pedidoEnviado() {
        ArgumentCaptor<MessageCreateParams> captor = ArgumentCaptor.forClass(MessageCreateParams.class);
        verify(anthropic.messages()).create(captor.capture());
        return captor.getValue();
    }
}
