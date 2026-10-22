package com.ensambla.backend.infrastructure.ia;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ImageBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ImageFormat;
import software.amazon.awssdk.services.bedrockruntime.model.ImageSource;

import java.util.Objects;
import java.util.stream.Collectors;

/** Bloques de la API Converse que arman y leen los clientes de Bedrock. */
final class BloquesBedrock {

    private BloquesBedrock() {
    }

    /** Una imagen en bytes; el formato sale del media type ({@code image/png} → {@code png}). */
    static ContentBlock imagen(String tipo, byte[] datos) {
        return ContentBlock.fromImage(ImageBlock.builder()
                .format(ImageFormat.fromValue(tipo.substring(tipo.indexOf('/') + 1)))
                .source(ImageSource.fromBytes(SdkBytes.fromByteArray(datos)))
                .build());
    }

    /** Todo el texto de la respuesta, sin los bloques que no son texto. */
    static String texto(ConverseResponse respuesta) {
        return respuesta.output().message().content().stream()
                .map(ContentBlock::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
    }
}
