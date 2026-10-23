package com.ensambla.backend.support;

import java.nio.charset.StandardCharsets;

/** Los primeros bytes de cada formato, los que usa {@code TipoFoto} para reconocerlos. */
public final class ImagenTestData {

    private ImagenTestData() {
    }

    public static byte[] jpeg() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    }

    public static byte[] png() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0, 0, 0, 0, 0};
    }

    public static byte[] webp() {
        return "RIFF....WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
    }

    public static byte[] pdf() {
        return "%PDF-1.7 contenido".getBytes(StandardCharsets.ISO_8859_1);
    }

    public static byte[] gif() {
        return "GIF89a contenido".getBytes(StandardCharsets.ISO_8859_1);
    }
}
