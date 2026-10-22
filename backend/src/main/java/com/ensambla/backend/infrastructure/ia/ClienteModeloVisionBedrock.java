package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Lectura de cuadernos con Claude en Amazon Bedrock (API Converse, cada página como {@code ImageBlock}). Lo arma
 * {@link ProveedorIaBedrock}. Igual que {@link ClienteModeloVisionAnthropic}: el esquema va en las instrucciones y la
 * validación estricta la hace {@link LectorCuadernoIaAdapter}.
 */
public class ClienteModeloVisionBedrock implements ClienteModeloVision {

    static final String PROVEEDOR = "bedrock";
    static final int MAX_TOKENS_SALIDA = 16_000;

    private final BedrockRuntimeClient client;
    private final String modelo;

    public ClienteModeloVisionBedrock(BedrockRuntimeClient client, String modelo) {
        this.client = client;
        this.modelo = modelo;
    }

    @Override
    public RespuestaModeloVision completar(PeticionModeloVision peticion) {
        ConverseResponse respuesta = client.converse(ConverseRequest.builder()
                .modelId(modelo)
                .system(SystemContentBlock.builder()
                        .text(TextoJson.instruccionesConEsquema(peticion.instrucciones(), peticion.esquemaJson()))
                        .build())
                .messages(Message.builder()
                        .role(ConversationRole.USER)
                        .content(contenidoUsuario(peticion))
                        .build())
                .inferenceConfig(config -> config.maxTokens(MAX_TOKENS_SALIDA))
                .build());
        return new RespuestaModeloVision(TextoJson.extraer(BloquesBedrock.texto(respuesta)), PROVEEDOR, modelo,
                respuesta.usage().inputTokens(), respuesta.usage().outputTokens());
    }

    private List<ContentBlock> contenidoUsuario(PeticionModeloVision peticion) {
        List<ContentBlock> bloques = new ArrayList<>();
        bloques.add(ContentBlock.fromText(peticion.contexto()));
        for (PaginaCuaderno pagina : peticion.paginas()) {
            bloques.add(ContentBlock.fromText("Página " + pagina.numero() + ":"));
            bloques.add(BloquesBedrock.imagen(pagina.tipoContenido(), pagina.contenido()));
        }
        return bloques;
    }
}
