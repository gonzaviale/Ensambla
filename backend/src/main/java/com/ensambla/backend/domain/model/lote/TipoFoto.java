package com.ensambla.backend.domain.model.lote;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Formatos que acepta la subida del cuaderno (los mismos que permite {@code foto_cuaderno.tipo_contenido}). El tipo se
 * detecta por los primeros bytes del archivo, no por lo que declara el navegador: cada formato sabe reconocerse.
 */
@Getter
@RequiredArgsConstructor
public enum TipoFoto {
    JPEG("image/jpeg", "jpg", TipoFoto::empiezaComoJpeg),
    PNG("image/png", "png", TipoFoto::empiezaComoPng),
    WEBP("image/webp", "webp", TipoFoto::empiezaComoWebp),
    HEIC("image/heic", "heic", TipoFoto::empiezaComoHeic),
    PDF("application/pdf", "pdf", TipoFoto::empiezaComoPdf);

    /** Marcas del contenedor ISO (bytes 8 a 11) que usan las fotos HEIC/HEIF de los celulares. */
    private static final Set<String> MARCAS_HEIC = Set.of(
            "heic", "heix", "hevc", "hevx", "heim", "heis", "mif1", "msf1");

    private final String mediaType;

    private final String extension;

    @Getter(AccessLevel.NONE)
    private final Predicate<byte[]> reconocedor;

    public static Optional<TipoFoto> detectar(byte[] contenido) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.reconoce(contenido))
                .findFirst();
    }

    private boolean reconoce(byte[] contenido) {
        return reconocedor.test(contenido);
    }

    private static boolean empiezaComoJpeg(byte[] contenido) {
        return texto(contenido, 0, 3).equals("ÿØÿ");
    }

    private static boolean empiezaComoPng(byte[] contenido) {
        return texto(contenido, 0, 4).equals("\u0089PNG");
    }

    private static boolean empiezaComoWebp(byte[] contenido) {
        return texto(contenido, 0, 4).equals("RIFF") && texto(contenido, 8, 4).equals("WEBP");
    }

    private static boolean empiezaComoHeic(byte[] contenido) {
        return texto(contenido, 4, 4).equals("ftyp") && MARCAS_HEIC.contains(texto(contenido, 8, 4));
    }

    private static boolean empiezaComoPdf(byte[] contenido) {
        return texto(contenido, 0, 5).equals("%PDF-");
    }

    private static String texto(byte[] contenido, int desde, int largo) {
        if (contenido.length < desde + largo) {
            return "";
        }
        return new String(contenido, desde, largo, StandardCharsets.ISO_8859_1);
    }
}
