package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ImageFormat;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;

import java.util.List;
import java.util.Map;

import static com.ensambla.backend.infrastructure.ia.RespuestasDePrueba.bedrockQueResponde;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class ClienteModeloVisionBedrockTest {

    private static final String MODELO = "us.anthropic.claude-haiku-4-5-20251001-v1:0";
    private static final PeticionModeloVision PETICION = new PeticionModeloVision("Leé el cuaderno", "Bloques: ninguno",
            List.of(new PaginaCuaderno(1, new byte[]{1, 2}, "image/jpeg"),
                    new PaginaCuaderno(2, new byte[]{3}, "image/png")), "{\"type\":\"object\"}");

    @Test
    @DisplayName("Devuelve el JSON sin la cerca de código, con los tokens y el modelo informados")
    void completarShouldReturnJsonWithTokens() {
        BedrockRuntimeClient bedrock = bedrockQueResponde("```json\n{\"a\":1}\n```");

        RespuestaModeloVision respuesta = new ClienteModeloVisionBedrock(bedrock, MODELO).completar(PETICION);

        assertThat(respuesta.json()).isEqualTo("{\"a\":1}");
        assertThat(respuesta.proveedor()).isEqualTo("bedrock");
        assertThat(respuesta.modelo()).isEqualTo(MODELO);
        assertThat(respuesta.tokensEntrada()).isEqualTo(RespuestasDePrueba.TOKENS_ENTRADA);
        assertThat(respuesta.tokensSalida()).isEqualTo(RespuestasDePrueba.TOKENS_SALIDA);
    }

    @Test
    @DisplayName("Ignora los bloques que no son texto al armar la respuesta")
    void completarShouldIgnoreNonTextBlocks() {
        BedrockRuntimeClient bedrock = bedrockQueResponde(
                ContentBlock.fromToolUse(ToolUseBlock.builder().toolUseId("t").name("x")
                        .input(Document.fromMap(Map.of())).build()),
                ContentBlock.fromText("{\"b\":2}"));

        RespuestaModeloVision respuesta = new ClienteModeloVisionBedrock(bedrock, MODELO).completar(PETICION);

        assertThat(respuesta.json()).isEqualTo("{\"b\":2}");
    }

    @Test
    @DisplayName("Manda el modelo, las instrucciones con el esquema, el contexto y cada página como ImageBlock")
    void completarShouldSendInstructionsContextAndPagesAsImages() {
        BedrockRuntimeClient bedrock = bedrockQueResponde("{}");

        new ClienteModeloVisionBedrock(bedrock, MODELO).completar(PETICION);

        ConverseRequest pedido = pedidoEnviado(bedrock);
        assertThat(pedido.modelId()).isEqualTo(MODELO);
        assertThat(pedido.system().getFirst().text()).startsWith("Leé el cuaderno").contains("{\"type\":\"object\"}");
        assertThat(pedido.messages().getFirst().role()).isEqualTo(ConversationRole.USER);
        List<ContentBlock> bloques = pedido.messages().getFirst().content();
        assertThat(bloques).extracting(ContentBlock::text)
                .containsExactly("Bloques: ninguno", "Página 1:", null, "Página 2:", null);
        assertThat(bloques.get(2).image().format()).isEqualTo(ImageFormat.JPEG);
        assertThat(bloques.get(2).image().source().bytes().asByteArray()).containsExactly(1, 2);
        assertThat(bloques.get(4).image().format()).isEqualTo(ImageFormat.PNG);
        assertThat(pedido.inferenceConfig().maxTokens()).isEqualTo(ClienteModeloVisionBedrock.MAX_TOKENS_SALIDA);
    }

    // --- assert ---
    private ConverseRequest pedidoEnviado(BedrockRuntimeClient bedrock) {
        ArgumentCaptor<ConverseRequest> captor = ArgumentCaptor.forClass(ConverseRequest.class);
        verify(bedrock).converse(captor.capture());
        return captor.getValue();
    }
}
