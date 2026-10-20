package com.ensambla.backend.infrastructure.ia;

import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.TextBlockParam;

import java.util.Base64;

/** Bloques de la API de mensajes que arman y leen los clientes de Anthropic. */
final class BloquesAnthropic {

    private BloquesAnthropic() {
    }

    static ContentBlockParam texto(String texto) {
        return ContentBlockParam.ofText(TextBlockParam.builder().text(texto).build());
    }

    /** Una imagen en base64 con su media type ({@code image/jpeg}, {@code image/png}…). */
    static ContentBlockParam imagen(String tipo, byte[] datos) {
        return ContentBlockParam.ofImage(ImageBlockParam.builder()
                .source(Base64ImageSource.builder()
                        .data(Base64.getEncoder().encodeToString(datos))
                        .mediaType(Base64ImageSource.MediaType.of(tipo))
                        .build())
                .build());
    }

    /** Todo el texto de la respuesta, sin los bloques que no son texto. */
    static String texto(Message mensaje) {
        StringBuilder texto = new StringBuilder();
        mensaje.content().forEach(bloque -> bloque.text().ifPresent(t -> texto.append(t.text())));
        return texto.toString();
    }
}
