package com.ensambla.backend.infrastructure.ia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClienteModeloTextoBedrockTest {

    private static final String MODELO = "us.anthropic.claude-haiku-4-5-20251001-v1:0";

    private BedrockRuntimeClient bedrock;
    private ClienteModeloTextoBedrock cliente;
    private RespuestaModeloTexto respuesta;

    @BeforeEach
    void setUp() {
        bedrock = mock(BedrockRuntimeClient.class);
        cliente = new ClienteModeloTextoBedrock(bedrock, MODELO, 700);
    }

    @Test
    @DisplayName("Manda las instrucciones con el esquema, el contexto y devuelve el JSON con los tokens")
    void mandaLasInstruccionesConElEsquemaElContextoYDevuelveElJsonConLosTokens() {
        dadoQueElModeloResponde("{\"plantilla\":\"Ventas\"}", 120, 15);

        cuandoSeCompletaConInstruccionesContextoYEsquema();

        entoncesDevuelveElJsonConLosTokens();
        entoncesElPedidoLlevaInstruccionesConEsquemaYContexto();
    }

    @Test
    @DisplayName("Descarta la cerca de código o el texto que rodea al JSON")
    void descartaLaCercaDeCodigoOElTextoQueRodeaAlJson() {
        dadoQueElModeloResponde("Acá va:\n```json\n{\"plantilla\":\"Ventas\"}\n```", 1, 1);

        cuandoSeCompleta();

        entoncesElJsonEs("{\"plantilla\":\"Ventas\"}");
    }

    @Test
    @DisplayName("Si la respuesta no tiene llaves la devuelve tal cual para que se rechace al validar")
    void siLaRespuestaNoTieneLlavesLaDevuelveTalCualParaQueSeRechaceAlValidar() {
        dadoQueElModeloResponde("no sé", 1, 1);

        cuandoSeCompleta();

        entoncesElJsonEs("no sé");
    }

    @Test
    @DisplayName("Si sólo hay una llave de apertura devuelve el texto tal cual")
    void siSoloHayUnaLlaveDeAperturaDevuelveElTextoTalCual() {
        dadoQueElModeloResponde("{", 1, 1);

        cuandoSeCompleta();

        entoncesElJsonEs("{");
    }

    @Test
    @DisplayName("Ignora los bloques de la respuesta que no son texto")
    void ignoraLosBloquesDeLaRespuestaQueNoSonTexto() {
        dadoUnModeloQueRespondeUnBloqueVacioYUnTexto();

        cuandoSeCompleta();

        entoncesElJsonEs("{\"a\":1}");
    }

    // --- dado ---

    private void dadoQueElModeloResponde(String texto, int tokensEntrada, int tokensSalida) {
        ConverseResponse converse = ConverseResponse.builder()
                .output(ConverseOutput.builder().message(Message.builder()
                        .role(ConversationRole.ASSISTANT)
                        .content(ContentBlock.fromText(texto))
                        .build()).build())
                .usage(TokenUsage.builder().inputTokens(tokensEntrada).outputTokens(tokensSalida).build())
                .build();
        when(bedrock.converse(any(ConverseRequest.class))).thenReturn(converse);
    }

    private void dadoUnModeloQueRespondeUnBloqueVacioYUnTexto() {
        ConverseResponse converse = ConverseResponse.builder()
                .output(ConverseOutput.builder().message(Message.builder()
                        .role(ConversationRole.ASSISTANT)
                        .content(ContentBlock.builder().build(), ContentBlock.fromText("{\"a\":1}"))
                        .build()).build())
                .usage(TokenUsage.builder().inputTokens(2).outputTokens(3).build())
                .build();
        when(bedrock.converse(any(ConverseRequest.class))).thenReturn(converse);
    }

    // --- cuando ---

    private void cuandoSeCompletaConInstruccionesContextoYEsquema() {
        respuesta = cliente.completar(new PeticionModeloTexto("Sos Sami", "contexto", "{schema}"));
    }

    private void cuandoSeCompleta() {
        respuesta = cliente.completar(new PeticionModeloTexto("i", "c", "{}"));
    }

    // --- entonces ---

    private void entoncesDevuelveElJsonConLosTokens() {
        assertThat(respuesta.json()).isEqualTo("{\"plantilla\":\"Ventas\"}");
        assertThat(respuesta.proveedor()).isEqualTo("bedrock");
        assertThat(respuesta.modelo()).isEqualTo(MODELO);
        assertThat(respuesta.tokensEntrada()).isEqualTo(120);
        assertThat(respuesta.tokensSalida()).isEqualTo(15);
    }

    private void entoncesElPedidoLlevaInstruccionesConEsquemaYContexto() {
        ConverseRequest pedido = pedidoEnviado();
        assertThat(pedido.modelId()).isEqualTo(MODELO);
        assertThat(pedido.system().getFirst().text()).startsWith("Sos Sami").contains("{schema}");
        assertThat(pedido.messages().getFirst().role()).isEqualTo(ConversationRole.USER);
        assertThat(pedido.messages().getFirst().content().getFirst().text()).isEqualTo("contexto");
        assertThat(pedido.inferenceConfig().maxTokens()).isEqualTo(700);
        assertThat(pedido.inferenceConfig().temperature()).isNull();
    }

    private void entoncesElJsonEs(String esperado) {
        assertThat(respuesta.json()).isEqualTo(esperado);
    }

    // --- datos ---

    private ConverseRequest pedidoEnviado() {
        ArgumentCaptor<ConverseRequest> captor = ArgumentCaptor.forClass(ConverseRequest.class);
        verify(bedrock).converse(captor.capture());
        return captor.getValue();
    }
}
