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

    @BeforeEach
    void setUp() {
        bedrock = mock(BedrockRuntimeClient.class);
        cliente = new ClienteModeloTextoBedrock(bedrock, MODELO, 700);
    }

    @Test
    @DisplayName("Manda las instrucciones con el esquema, el contexto y devuelve el JSON con los tokens")
    void completarShouldSendConverseRequestAndReturnJsonWithTokens() {
        givenModelAnswers("{\"plantilla\":\"Ventas\"}", 120, 15);

        RespuestaModeloTexto result = cliente.completar(new PeticionModeloTexto("Sos Sami", "contexto", "{schema}"));

        assertThat(result.json()).isEqualTo("{\"plantilla\":\"Ventas\"}");
        assertThat(result.proveedor()).isEqualTo("bedrock");
        assertThat(result.modelo()).isEqualTo(MODELO);
        assertThat(result.tokensEntrada()).isEqualTo(120);
        assertThat(result.tokensSalida()).isEqualTo(15);
        ConverseRequest pedido = capturedRequest();
        assertThat(pedido.modelId()).isEqualTo(MODELO);
        assertThat(pedido.system().getFirst().text()).startsWith("Sos Sami").contains("{schema}");
        assertThat(pedido.messages().getFirst().role()).isEqualTo(ConversationRole.USER);
        assertThat(pedido.messages().getFirst().content().getFirst().text()).isEqualTo("contexto");
        assertThat(pedido.inferenceConfig().maxTokens()).isEqualTo(700);
        assertThat(pedido.inferenceConfig().temperature()).isZero();
    }

    @Test
    @DisplayName("Descarta la cerca de código o el texto que rodea al JSON")
    void completarShouldStripTextAroundJson() {
        givenModelAnswers("Acá va:\n```json\n{\"plantilla\":\"Ventas\"}\n```", 1, 1);

        RespuestaModeloTexto result = cliente.completar(new PeticionModeloTexto("i", "c", "{}"));

        assertThat(result.json()).isEqualTo("{\"plantilla\":\"Ventas\"}");
    }

    @Test
    @DisplayName("Si la respuesta no tiene llaves la devuelve tal cual para que se rechace al validar")
    void completarShouldReturnTextAsIsWhenThereAreNoBraces() {
        givenModelAnswers("no sé", 1, 1);

        assertThat(cliente.completar(new PeticionModeloTexto("i", "c", "{}")).json()).isEqualTo("no sé");
    }

    @Test
    @DisplayName("Si sólo hay una llave de apertura devuelve el texto tal cual")
    void completarShouldReturnTextAsIsWhenBracesAreUnbalanced() {
        givenModelAnswers("{", 1, 1);

        assertThat(cliente.completar(new PeticionModeloTexto("i", "c", "{}")).json()).isEqualTo("{");
    }

    @Test
    @DisplayName("Ignora los bloques de la respuesta que no son texto")
    void completarShouldIgnoreNonTextBlocks() {
        ConverseResponse respuesta = ConverseResponse.builder()
                .output(ConverseOutput.builder().message(Message.builder()
                        .role(ConversationRole.ASSISTANT)
                        .content(ContentBlock.builder().build(), ContentBlock.fromText("{\"a\":1}"))
                        .build()).build())
                .usage(TokenUsage.builder().inputTokens(2).outputTokens(3).build())
                .build();
        when(bedrock.converse(any(ConverseRequest.class))).thenReturn(respuesta);

        assertThat(cliente.completar(new PeticionModeloTexto("i", "c", "{}")).json()).isEqualTo("{\"a\":1}");
    }

    private void givenModelAnswers(String texto, int tokensEntrada, int tokensSalida) {
        ConverseResponse respuesta = ConverseResponse.builder()
                .output(ConverseOutput.builder().message(Message.builder()
                        .role(ConversationRole.ASSISTANT)
                        .content(ContentBlock.fromText(texto))
                        .build()).build())
                .usage(TokenUsage.builder().inputTokens(tokensEntrada).outputTokens(tokensSalida).build())
                .build();
        when(bedrock.converse(any(ConverseRequest.class))).thenReturn(respuesta);
    }

    private ConverseRequest capturedRequest() {
        ArgumentCaptor<ConverseRequest> captor = ArgumentCaptor.forClass(ConverseRequest.class);
        verify(bedrock).converse(captor.capture());
        return captor.getValue();
    }
}
