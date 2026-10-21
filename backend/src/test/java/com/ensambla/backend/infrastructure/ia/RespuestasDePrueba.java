package com.ensambla.backend.infrastructure.ia;

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Clientes de Bedrock simulados con Mockito que contestan siempre lo mismo, para los tests de configuración. */
final class RespuestasDePrueba {

    static final int TOKENS_ENTRADA = 10;
    static final int TOKENS_SALIDA = 5;

    private RespuestasDePrueba() {
    }

    /** Un cliente de Bedrock cuyo modelo contesta los bloques indicados. */
    static BedrockRuntimeClient bedrockQueResponde(ContentBlock... bloques) {
        BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);
        when(bedrock.converse(any(ConverseRequest.class))).thenReturn(respuestaBedrock(bloques));
        return bedrock;
    }

    static BedrockRuntimeClient bedrockQueResponde(String texto) {
        return bedrockQueResponde(ContentBlock.fromText(texto));
    }

    static ConverseResponse respuestaBedrock(ContentBlock... bloques) {
        return ConverseResponse.builder()
                .output(ConverseOutput.builder().message(Message.builder()
                        .role(ConversationRole.ASSISTANT).content(bloques).build()).build())
                .usage(TokenUsage.builder().inputTokens(TOKENS_ENTRADA).outputTokens(TOKENS_SALIDA).build())
                .build();
    }
}
