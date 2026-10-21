package com.ensambla.backend.domain.model.sami.taller;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.Base64;
import java.util.Set;

/**
 * Una foto que la persona adjunta en un turno del taller (un cuaderno, una lista, una planilla) para que Sami arme las
 * piezas y cargue lo que se lee. Sólo JPG, PNG o WEBP y hasta {@link #MAX_BYTES}; si no, se rechaza con un mensaje
 * para la persona.
 *
 * @param tipo  media type ({@code image/jpeg}, {@code image/png}, {@code image/webp})
 * @param datos los bytes de la imagen
 */
public record ImagenTaller(String tipo, byte[] datos) {

    public static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp");
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    /** Lo que ocupa en base64 una imagen de {@link #MAX_BYTES}: más largo ya se pasa, sin decodificarlo. */
    static final int MAX_LARGO_BASE64 = (MAX_BYTES + 2) / 3 * 4;

    public static final String TIPO_INVALIDO = "La foto tiene que ser JPG, PNG o WEBP.";
    public static final String MUY_GRANDE = "La foto pesa más de 5 MB. Probá con una más liviana.";
    public static final String ILEGIBLE = "No pudimos abrir la foto. Probá adjuntarla de nuevo.";

    public ImagenTaller {
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new BusinessRuleException(TIPO_INVALIDO);
        }
        if (datos == null || datos.length == 0) {
            throw new BusinessRuleException(ILEGIBLE);
        }
        if (datos.length > MAX_BYTES) {
            throw new BusinessRuleException(MUY_GRANDE);
        }
    }

    /**
     * La imagen a partir de su base64. Acepta también una data URL ({@code data:image/png;base64,...}), que es lo que
     * entrega el navegador al leer un archivo.
     */
    public static ImagenTaller desdeBase64(String tipo, String base64) {
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new BusinessRuleException(TIPO_INVALIDO);
        }
        if (base64 == null) {
            throw new BusinessRuleException(ILEGIBLE);
        }
        String datos = base64.substring(base64.indexOf(',') + 1).strip();
        if (datos.length() > MAX_LARGO_BASE64) {
            throw new BusinessRuleException(MUY_GRANDE);
        }
        try {
            return new ImagenTaller(tipo, Base64.getDecoder().decode(datos));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(ILEGIBLE);
        }
    }
}
