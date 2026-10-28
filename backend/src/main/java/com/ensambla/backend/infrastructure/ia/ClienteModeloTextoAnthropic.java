package com.ensambla.backend.infrastructure.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

/**
 * Sami con Claude de la API de Anthropic: texto a JSON. Lo arma {@link ProveedorIaAnthropic}. Como
 * {@link ClienteModeloTextoBedrock}: el esquema va en las instrucciones, la validación la hace {@link SamiIaAdapter}.
 * No se manda {@code temperature}: los modelos nuevos (Haiku 5.5) rechazan cualquier valor que no sea el default.
 */
public class ClienteModeloTextoAnthropic implements ClienteModeloTexto {

    static final String PROVEEDOR = "anthropic";

    private final AnthropicClient client;
    private final String modelo;
    private final int maxTokens;

    public ClienteModeloTextoAnthropic(AnthropicClient client, String modelo, int maxTokens) {
        this.client = client;
        this.modelo = modelo;
        this.maxTokens = maxTokens;
    }

    @Override
    public RespuestaModeloTexto completar(PeticionModeloTexto peticion) {
        Message mensaje = client.messages().create(MessageCreateParams.builder()
                .model(modelo)
                .maxTokens(maxTokens)
                .system(TextoJson.instruccionesConEsquema(peticion.instrucciones(), peticion.esquemaJson()))
                .addUserMessage(peticion.contexto())
                .build());
        return new RespuestaModeloTexto(TextoJson.extraer(BloquesAnthropic.texto(mensaje)), PROVEEDOR, modelo,
                mensaje.usage().inputTokens(), mensaje.usage().outputTokens());
    }
}
