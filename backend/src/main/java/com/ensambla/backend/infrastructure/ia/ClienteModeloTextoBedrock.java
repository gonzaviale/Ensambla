package com.ensambla.backend.infrastructure.ia;

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Claude en Amazon Bedrock con la API Converse. Se activa con {@code ensambla.sami.ia.proveedor=bedrock}.
 * <p>
 * El esquema va dentro de las instrucciones (igual que en {@link ClienteModeloVisionAnthropic}); la validación
 * estricta la hace {@link SamiIaAdapter}. La temperatura es 0 para que la misma descripción dé la misma propuesta.
 */
public class ClienteModeloTextoBedrock implements ClienteModeloTexto {

    static final String PROVEEDOR = "bedrock";

    private final BedrockRuntimeClient client;
    private final String modelo;
    private final int maxTokens;

    public ClienteModeloTextoBedrock(BedrockRuntimeClient client, String modelo, int maxTokens) {
        this.client = client;
        this.modelo = modelo;
        this.maxTokens = maxTokens;
    }

    @Override
    public RespuestaModeloTexto completar(PeticionModeloTexto peticion) {
        ConverseResponse respuesta = client.converse(ConverseRequest.builder()
                .modelId(modelo)
                .system(SystemContentBlock.builder().text(instrucciones(peticion)).build())
                .messages(Message.builder()
                        .role(ConversationRole.USER)
                        .content(ContentBlock.fromText(peticion.contexto()))
                        .build())
                .inferenceConfig(config -> config.maxTokens(maxTokens).temperature(0f))
                .build());
        return new RespuestaModeloTexto(
                extraerJson(textoDe(respuesta)),
                PROVEEDOR,
                modelo,
                respuesta.usage().inputTokens(),
                respuesta.usage().outputTokens());
    }

    private String instrucciones(PeticionModeloTexto peticion) {
        return peticion.instrucciones()
                + "\n\nRespondé únicamente con un objeto JSON válido que cumpla este JSON Schema, sin texto"
                + " antes ni después y sin bloques de código:\n" + peticion.esquemaJson();
    }

    private String textoDe(ConverseResponse respuesta) {
        return respuesta.output().message().content().stream()
                .map(ContentBlock::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
    }

    /** Descarta cualquier cerca de código o comentario alrededor del objeto JSON. */
    private String extraerJson(String texto) {
        int inicio = texto.indexOf('{');
        int fin = texto.lastIndexOf('}');
        return inicio >= 0 && fin > inicio ? texto.substring(inicio, fin + 1) : texto;
    }
}
