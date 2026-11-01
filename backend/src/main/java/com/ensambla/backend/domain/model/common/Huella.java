package com.ensambla.backend.domain.model.common;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Huella estable de un texto: un UUID derivado (MD5 de 128 bits). Alcanza para distinguir textos distintos, da siempre
 * lo mismo para el mismo texto y no puede fallar. Sirve para claves deterministas, no para identificar entidades.
 */
public final class Huella {

    private Huella() {
    }

    public static String de(String texto) {
        return UUID.nameUUIDFromBytes(texto.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
