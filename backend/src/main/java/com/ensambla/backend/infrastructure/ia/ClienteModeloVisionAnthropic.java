package com.ensambla.backend.infrastructure.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;

import java.util.ArrayList;
import java.util.List;

/**
 * Cliente de lectura con Claude (API de Anthropic). Lo arma {@link ProveedorIaAnthropic}.
 * <p>
 * El esquema del resultado va dentro de las instrucciones y no como salida estructurada del proveedor, porque
 * {@code esquema-lectura-cuaderno.json} usa construcciones que ésta no admite (patrones, rangos, mapas abiertos).
 * La validación estricta la hace {@link LectorCuadernoIaAdapter} sobre el JSON devuelto.
 */
public class ClienteModeloVisionAnthropic implements ClienteModeloVision {

    static final String PROVEEDOR = "anthropic";
    static final long MAX_TOKENS_SALIDA = 16_000L;

    private final AnthropicClient client;
    private final String modelo;

    public ClienteModeloVisionAnthropic(AnthropicClient client, String modelo) {
        this.client = client;
        this.modelo = modelo;
    }

    @Override
    public RespuestaModeloVision completar(PeticionModeloVision peticion) {
        Message mensaje = client.messages().create(armarParametros(peticion));
        return new RespuestaModeloVision(TextoJson.extraer(BloquesAnthropic.texto(mensaje)), PROVEEDOR, modelo,
                (int) mensaje.usage().inputTokens(), (int) mensaje.usage().outputTokens());
    }

    private MessageCreateParams armarParametros(PeticionModeloVision peticion) {
        return MessageCreateParams.builder()
                .model(modelo)
                .maxTokens(MAX_TOKENS_SALIDA)
                .system(TextoJson.instruccionesConEsquema(peticion.instrucciones(), peticion.esquemaJson()))
                .addUserMessageOfBlockParams(contenidoUsuario(peticion))
                .build();
    }

    private List<ContentBlockParam> contenidoUsuario(PeticionModeloVision peticion) {
        List<ContentBlockParam> bloques = new ArrayList<>();
        bloques.add(BloquesAnthropic.texto(peticion.contexto()));
        for (PaginaCuaderno pagina : peticion.paginas()) {
            bloques.add(BloquesAnthropic.texto("Página " + pagina.numero() + ":"));
            bloques.add(BloquesAnthropic.imagen(pagina.tipoContenido(), pagina.contenido()));
        }
        return bloques;
    }
}
