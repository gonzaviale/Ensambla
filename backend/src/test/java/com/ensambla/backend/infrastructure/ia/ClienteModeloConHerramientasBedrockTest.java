package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ImageFormat;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClienteModeloConHerramientasBedrockTest {

    private static final String MODELO = "us.anthropic.claude-haiku-4-5-20251001-v1:0";

    private BedrockRuntimeClient bedrock;
    private ClienteModeloConHerramientasBedrock cliente;

    @BeforeEach
    void setUp() {
        bedrock = mock(BedrockRuntimeClient.class);
        cliente = new ClienteModeloConHerramientasBedrock(bedrock, MODELO, 400, List.of(
                new DefinicionHerramienta("agregar_pieza", "Suma una pieza", Map.of(
                        "type", "object",
                        "properties", Map.of("tipo", Map.of("type", "string")),
                        "required", List.of("tipo")))));
    }

    @Test
    @DisplayName("Manda las instrucciones, el contexto y las herramientas, y devuelve texto, llamadas y tokens")
    void completarShouldSendToolsAndReturnTextCallsAndTokens() {
        givenModelAnswers(
                ContentBlock.fromText("Listo, sumé los gastos."),
                ContentBlock.fromToolUse(ToolUseBlock.builder()
                        .toolUseId("t1")
                        .name("agregar_pieza")
                        .input(Document.fromMap(Map.of("tipo", Document.fromString("expenses"))))
                        .build()));

        RespuestaConHerramientas result = cliente.completar(new PeticionConHerramientas("Sos Sami", "contexto"));

        assertThat(result.texto()).isEqualTo("Listo, sumé los gastos.");
        assertThat(result.llamadas()).containsExactly(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses")));
        assertThat(result.proveedor()).isEqualTo("bedrock");
        assertThat(result.modelo()).isEqualTo(MODELO);
        assertThat(result.tokensEntrada()).isEqualTo(850);
        assertThat(result.tokensSalida()).isEqualTo(70);
        ConverseRequest pedido = capturedRequest();
        assertThat(pedido.modelId()).isEqualTo(MODELO);
        assertThat(pedido.system().getFirst().text()).isEqualTo("Sos Sami");
        assertThat(pedido.messages().getFirst().role()).isEqualTo(ConversationRole.USER);
        assertThat(pedido.messages().getFirst().content().getFirst().text()).isEqualTo("contexto");
        assertThat(pedido.inferenceConfig().maxTokens()).isEqualTo(400);
        assertThat(pedido.inferenceConfig().temperature()).isZero();
        assertThat(pedido.toolConfig().tools()).hasSize(1);
        assertThat(pedido.toolConfig().tools().getFirst().toolSpec().name()).isEqualTo("agregar_pieza");
        assertThat(pedido.toolConfig().tools().getFirst().toolSpec().inputSchema().json().asMap())
                .containsKeys("type", "properties", "required");
    }

    @Test
    @DisplayName("Si el modelo sólo pide una herramienta, el texto queda vacío")
    void completarShouldReturnEmptyTextWhenModelOnlyCallsATool() {
        givenModelAnswers(ContentBlock.fromToolUse(ToolUseBlock.builder()
                .toolUseId("t1")
                .name("ofrecer_opciones")
                .input(Document.fromMap(Map.of(
                        "pregunta", Document.fromString("¿Cuál?"),
                        "opciones", Document.fromList(List.of(Document.fromString("A"), Document.fromString("B"))))))
                .build()));

        RespuestaConHerramientas result = cliente.completar(new PeticionConHerramientas("i", "c"));

        assertThat(result.texto()).isEmpty();
        assertThat(result.llamadas().getFirst().argumentos())
                .containsEntry("pregunta", "¿Cuál?")
                .containsEntry("opciones", List.of("A", "B"));
    }

    @Test
    @DisplayName("Si el modelo sólo conversa no hay llamadas")
    void completarShouldReturnNoCallsWhenModelOnlyTalks() {
        givenModelAnswers(ContentBlock.fromText("Claro, contame más."));

        RespuestaConHerramientas result = cliente.completar(new PeticionConHerramientas("i", "c"));

        assertThat(result.texto()).isEqualTo("Claro, contame más.");
        assertThat(result.llamadas()).isEmpty();
    }

    @Test
    @DisplayName("La foto adjunta va en el mismo mensaje como ImageBlock, antes del texto del turno")
    void completarShouldSendAttachedImageAsImageBlock() {
        givenModelAnswers(ContentBlock.fromText("Leí la foto."));

        cliente.completar(new PeticionConHerramientas("i", "contexto",
                List.of(new ImagenTaller("image/webp", new byte[]{4, 5}))));

        List<ContentBlock> bloques = capturedRequest().messages().getFirst().content();
        assertThat(bloques).hasSize(2);
        assertThat(bloques.getFirst().image().format()).isEqualTo(ImageFormat.WEBP);
        assertThat(bloques.getFirst().image().source().bytes().asByteArray()).containsExactly(4, 5);
        assertThat(bloques.get(1).text()).isEqualTo("contexto");
    }

    @Test
    @DisplayName("Si el modelo se queda sin tokens igual devuelve lo que alcanzó a decir")
    void completarShouldStillAnswerWhenTheModelRunsOutOfTokens() {
        when(bedrock.converse(any(ConverseRequest.class))).thenReturn(ConverseResponse.builder()
                .output(ConverseOutput.builder().message(Message.builder()
                        .role(ConversationRole.ASSISTANT).content(ContentBlock.fromText("Veo un cuaderno")).build()).build())
                .stopReason(StopReason.MAX_TOKENS)
                .usage(TokenUsage.builder().inputTokens(850).outputTokens(400).build())
                .build());

        RespuestaConHerramientas result = cliente.completar(new PeticionConHerramientas("i", "c"));

        assertThat(result.texto()).isEqualTo("Veo un cuaderno");
        assertThat(result.llamadas()).isEmpty();
    }

    private void givenModelAnswers(ContentBlock... bloques) {
        when(bedrock.converse(any(ConverseRequest.class))).thenReturn(ConverseResponse.builder()
                .output(ConverseOutput.builder().message(Message.builder()
                        .role(ConversationRole.ASSISTANT).content(bloques).build()).build())
                .usage(TokenUsage.builder().inputTokens(850).outputTokens(70).build())
                .build());
    }

    private ConverseRequest capturedRequest() {
        ArgumentCaptor<ConverseRequest> captor = ArgumentCaptor.forClass(ConverseRequest.class);
        verify(bedrock).converse(captor.capture());
        return captor.getValue();
    }
}
