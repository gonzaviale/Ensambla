package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;
import software.amazon.awssdk.services.bedrockruntime.model.ToolConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.ToolInputSchema;
import software.amazon.awssdk.services.bedrockruntime.model.ToolSpecification;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Claude en Amazon Bedrock con la API Converse y herramientas (tool use). Hace <b>una sola llamada</b>: el modelo
 * contesta con texto y con las herramientas que quiere invocar, y ahí termina; no se le devuelve ningún resultado.
 * Lo arma {@link ProveedorIaBedrock}; las fotos van como {@code ImageBlock} antes del texto del turno.
 */
public class ClienteModeloConHerramientasBedrock implements ClienteModeloConHerramientas {

    static final String PROVEEDOR = "bedrock";
    private static final Logger log = LoggerFactory.getLogger(ClienteModeloConHerramientasBedrock.class);

    private final BedrockRuntimeClient client;
    private final String modelo;
    private final int maxTokens;
    private final ToolConfiguration herramientas;

    public ClienteModeloConHerramientasBedrock(BedrockRuntimeClient client, String modelo, int maxTokens,
                                               List<DefinicionHerramienta> definiciones) {
        this.client = client;
        this.modelo = modelo;
        this.maxTokens = maxTokens;
        this.herramientas = ToolConfiguration.builder()
                .tools(definiciones.stream().map(this::aHerramienta).toList())
                .build();
    }

    @Override
    public RespuestaConHerramientas completar(PeticionConHerramientas peticion) {
        ConverseResponse respuesta = client.converse(ConverseRequest.builder()
                .modelId(modelo)
                .system(SystemContentBlock.builder().text(peticion.instrucciones()).build())
                .messages(Message.builder()
                        .role(ConversationRole.USER)
                        .content(contenidoUsuario(peticion))
                        .build())
                .toolConfig(herramientas)
                .inferenceConfig(config -> config.maxTokens(maxTokens).temperature(0f))
                .build());
        if (respuesta.stopReason() == StopReason.MAX_TOKENS) {
            log.warn("El modelo del taller se quedó sin tokens ({}): puede haber herramientas cortadas o sin filas",
                    maxTokens);
        }
        StringBuilder texto = new StringBuilder();
        List<LlamadaHerramienta> llamadas = new ArrayList<>();
        for (ContentBlock bloque : respuesta.output().message().content()) {
            if (bloque.text() != null) {
                texto.append(bloque.text());
            }
            ToolUseBlock uso = bloque.toolUse();
            if (uso != null) {
                llamadas.add(new LlamadaHerramienta(uso.name(), argumentosDe(uso)));
            }
        }
        return new RespuestaConHerramientas(texto.toString(), llamadas, PROVEEDOR, modelo,
                respuesta.usage().inputTokens(), respuesta.usage().outputTokens());
    }

    /** Las fotos van primero (como {@code ImageBlock}) y después el texto del turno. */
    private List<ContentBlock> contenidoUsuario(PeticionConHerramientas peticion) {
        List<ContentBlock> bloques = new ArrayList<>();
        peticion.imagenes().forEach(imagen -> bloques.add(BloquesBedrock.imagen(imagen.tipo(), imagen.datos())));
        bloques.add(ContentBlock.fromText(peticion.contexto()));
        return bloques;
    }

    private Tool aHerramienta(DefinicionHerramienta definicion) {
        return Tool.fromToolSpec(ToolSpecification.builder()
                .name(definicion.name())
                .description(definicion.description())
                .inputSchema(ToolInputSchema.fromJson(DocumentosAws.aDocumento(definicion.inputSchema())))
                .build());
    }

    private Map<String, Object> argumentosDe(ToolUseBlock uso) {
        Map<String, Object> argumentos = new LinkedHashMap<>();
        uso.input().asMap().forEach((nombre, valor) -> argumentos.put(nombre, DocumentosAws.aObjeto(valor)));
        return argumentos;
    }
}
