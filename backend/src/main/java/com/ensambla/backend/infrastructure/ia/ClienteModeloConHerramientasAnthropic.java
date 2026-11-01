package com.ensambla.backend.infrastructure.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolUseBlock;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Claude de la API de Anthropic con herramientas (tool use): el taller de Sami cuando el proveedor es
 * {@code anthropic}. Como {@link ClienteModeloConHerramientasBedrock}, hace <b>una sola llamada</b>: el modelo contesta
 * con texto y con las herramientas que quiere invocar ({@code tool_use}), y ahí termina.
 */
public class ClienteModeloConHerramientasAnthropic implements ClienteModeloConHerramientas {

    static final String PROVEEDOR = "anthropic";
    private static final TypeReference<Map<String, Object>> ARGUMENTOS = new TypeReference<>() {
    };

    private final AnthropicClient client;
    private final String modelo;
    private final int maxTokens;
    private final List<Tool> herramientas;

    public ClienteModeloConHerramientasAnthropic(AnthropicClient client, String modelo, int maxTokens,
                                                 List<DefinicionHerramienta> definiciones) {
        this.client = client;
        this.modelo = modelo;
        this.maxTokens = maxTokens;
        this.herramientas = definiciones.stream().map(ClienteModeloConHerramientasAnthropic::aHerramienta).toList();
    }

    @Override
    public RespuestaConHerramientas completar(PeticionConHerramientas peticion) {
        MessageCreateParams.Builder pedido = MessageCreateParams.builder()
                .model(modelo)
                .maxTokens(maxTokens)
                .system(peticion.instrucciones())
                .addUserMessageOfBlockParams(contenidoUsuario(peticion));
        herramientas.forEach(pedido::addTool);
        Message mensaje = client.messages().create(pedido.build());
        List<LlamadaHerramienta> llamadas = new ArrayList<>();
        for (ContentBlock bloque : mensaje.content()) {
            bloque.toolUse().ifPresent(uso -> llamadas.add(new LlamadaHerramienta(uso.name(), argumentosDe(uso))));
        }
        return new RespuestaConHerramientas(BloquesAnthropic.texto(mensaje), llamadas, PROVEEDOR, modelo,
                mensaje.usage().inputTokens(), mensaje.usage().outputTokens());
    }

    /** Las fotos van primero (como bloques de imagen) y después el texto del turno. */
    private List<ContentBlockParam> contenidoUsuario(PeticionConHerramientas peticion) {
        List<ContentBlockParam> bloques = new ArrayList<>();
        peticion.imagenes().forEach(imagen -> bloques.add(BloquesAnthropic.imagen(imagen.tipo(), imagen.datos())));
        bloques.add(BloquesAnthropic.texto(peticion.contexto()));
        return bloques;
    }

    /** {@code herramientas-taller.json} usa JSON Schema: {@code properties} y {@code required} pasan tal cual. */
    static Tool aHerramienta(DefinicionHerramienta definicion) {
        Map<String, Object> esquema = definicion.inputSchema();
        Tool.InputSchema.Properties.Builder propiedades = Tool.InputSchema.Properties.builder();
        if (esquema.get("properties") instanceof Map<?, ?> campos) {
            campos.forEach((nombre, tipo) -> propiedades.putAdditionalProperty(String.valueOf(nombre),
                    JsonValue.from(tipo)));
        }
        Tool.InputSchema.Builder entrada = Tool.InputSchema.builder().properties(propiedades.build());
        if (esquema.get("required") instanceof List<?> requeridos) {
            entrada.required(requeridos.stream().map(String::valueOf).toList());
        }
        return Tool.builder()
                .name(definicion.name())
                .description(definicion.description())
                .inputSchema(entrada.build())
                .build();
    }

    /** Los números llegan como {@code Integer}, {@code Long} o {@code Double}; el dominio acepta cualquier número. */
    private Map<String, Object> argumentosDe(ToolUseBlock uso) {
        Map<String, Object> argumentos = uso._input().convert(ARGUMENTOS);
        return argumentos == null ? Map.of() : new LinkedHashMap<>(argumentos);
    }
}
