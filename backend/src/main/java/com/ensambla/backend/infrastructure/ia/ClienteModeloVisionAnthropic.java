package com.ensambla.backend.infrastructure.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Cliente de lectura con Claude (API de Anthropic). Se activa con {@code ensambla.ia.proveedor=anthropic}.
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
        return new RespuestaModeloVision(extraerJson(textoDe(mensaje)), PROVEEDOR, modelo,
                (int) mensaje.usage().inputTokens(), (int) mensaje.usage().outputTokens());
    }

    private MessageCreateParams armarParametros(PeticionModeloVision peticion) {
        return MessageCreateParams.builder()
                .model(modelo)
                .maxTokens(MAX_TOKENS_SALIDA)
                .system(instrucciones(peticion))
                .addUserMessageOfBlockParams(contenidoUsuario(peticion))
                .build();
    }

    private String instrucciones(PeticionModeloVision peticion) {
        return peticion.instrucciones()
                + "\n\nRespondé únicamente con un objeto JSON válido que cumpla este JSON Schema, sin texto"
                + " antes ni después y sin bloques de código:\n" + peticion.esquemaJson();
    }

    private List<ContentBlockParam> contenidoUsuario(PeticionModeloVision peticion) {
        List<ContentBlockParam> bloques = new ArrayList<>();
        bloques.add(texto(peticion.contexto()));
        for (PaginaCuaderno pagina : peticion.paginas()) {
            bloques.add(texto("Página " + pagina.numero() + ":"));
            bloques.add(imagen(pagina));
        }
        return bloques;
    }

    private ContentBlockParam texto(String texto) {
        return ContentBlockParam.ofText(TextBlockParam.builder().text(texto).build());
    }

    private ContentBlockParam imagen(PaginaCuaderno pagina) {
        return ContentBlockParam.ofImage(ImageBlockParam.builder()
                .source(Base64ImageSource.builder()
                        .data(Base64.getEncoder().encodeToString(pagina.contenido()))
                        .mediaType(Base64ImageSource.MediaType.of(pagina.tipoContenido()))
                        .build())
                .build());
    }

    private String textoDe(Message mensaje) {
        StringBuilder texto = new StringBuilder();
        mensaje.content().forEach(bloque -> bloque.text().ifPresent(t -> texto.append(t.text())));
        return texto.toString();
    }

    /** Descarta cualquier cerca de código o comentario alrededor del objeto JSON. */
    private String extraerJson(String texto) {
        int inicio = texto.indexOf('{');
        int fin = texto.lastIndexOf('}');
        return inicio >= 0 && fin > inicio ? texto.substring(inicio, fin + 1) : texto;
    }
}
