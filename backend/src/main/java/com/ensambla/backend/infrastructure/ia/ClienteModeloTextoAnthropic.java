package com.ensambla.backend.infrastructure.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

/**
 * Sami con Claude de la API de Anthropic: texto a JSON. Lo arma {@link ProveedorIaAnthropic}. Como
 * {@link ClienteModeloTextoBedrock}: el esquema va en las instrucciones, la validación la hace {@link SamiIaAdapter}
 * y la temperatura es 0 para que la misma descripción dé la misma propuesta.
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
                .temperature(0.0)
                .system(TextoJson.instruccionesConEsquema(peticion.instrucciones(), peticion.esquemaJson()))
                .addUserMessage(peticion.contexto())
                .build());
        return new RespuestaModeloTexto(TextoJson.extraer(BloquesAnthropic.texto(mensaje)), PROVEEDOR, modelo,
                mensaje.usage().inputTokens(), mensaje.usage().outputTokens());
    }
}
