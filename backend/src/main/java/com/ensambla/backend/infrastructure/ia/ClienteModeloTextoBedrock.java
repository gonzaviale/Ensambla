package com.ensambla.backend.infrastructure.ia;

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

/**
 * Claude en Amazon Bedrock con la API Converse. Lo arma {@link ProveedorIaBedrock}.
 * <p>
 * El esquema va dentro de las instrucciones (igual que en {@link ClienteModeloVisionAnthropic}); la validación
 * estricta la hace {@link SamiIaAdapter}. No se manda {@code temperature}: los modelos nuevos la rechazan.
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
                .system(SystemContentBlock.builder()
                        .text(TextoJson.instruccionesConEsquema(peticion.instrucciones(), peticion.esquemaJson()))
                        .build())
                .messages(Message.builder()
                        .role(ConversationRole.USER)
                        .content(ContentBlock.fromText(peticion.contexto()))
                        .build())
                .inferenceConfig(config -> config.maxTokens(maxTokens))
                .build());
        return new RespuestaModeloTexto(
                TextoJson.extraer(BloquesBedrock.texto(respuesta)),
                PROVEEDOR,
                modelo,
                respuesta.usage().inputTokens(),
                respuesta.usage().outputTokens());
    }
}
