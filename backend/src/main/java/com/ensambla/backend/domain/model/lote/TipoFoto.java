package com.ensambla.backend.domain.model.lote;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Formatos que acepta la subida del cuaderno (los mismos que permite {@code foto_cuaderno.tipo_contenido}). El tipo se
 * detecta por los primeros bytes del archivo, no por lo que declara el navegador.
 */
@Getter
@RequiredArgsConstructor
public enum TipoFoto {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    HEIC("image/heic", "heic"),
    PDF("application/pdf", "pdf");

    /** Marcas del contenedor ISO (bytes 8 a 11) que usan las fotos HEIC/HEIF de los celulares. */
    private static final Set<String> MARCAS_HEIC = Set.of(
            "heic", "heix", "hevc", "hevx", "heim", "heis", "mif1", "msf1");

    private final String mediaType;

    private final String extension;

    public static Optional<TipoFoto> detectar(byte[] contenido) {
        if (texto(contenido, 0, 3).equals("\u00FF\u00D8\u00FF")) {
            return Optional.of(JPEG);
        }
        if (texto(contenido, 0, 4).equals("\u0089PNG")) {
            return Optional.of(PNG);
        }
        if (texto(contenido, 0, 4).equals("RIFF") && texto(contenido, 8, 4).equals("WEBP")) {
            return Optional.of(WEBP);
        }
        if (texto(contenido, 4, 4).equals("ftyp") && MARCAS_HEIC.contains(texto(contenido, 8, 4))) {
            return Optional.of(HEIC);
        }
        if (texto(contenido, 0, 5).equals("%PDF-")) {
            return Optional.of(PDF);
        }
        return Optional.empty();
    }

    private static String texto(byte[] contenido, int desde, int largo) {
        if (contenido.length < desde + largo) {
            return "";
        }
        return new String(contenido, desde, largo, StandardCharsets.ISO_8859_1);
    }
}
