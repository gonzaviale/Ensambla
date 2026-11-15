package com.ensambla.backend.domain.model.marca;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.Set;

/** El logo del negocio: una imagen PNG o JPG de hasta 2 MB. */
public record LogoDeMarca(byte[] contenido, String tipoMime) {

    public static final int TAMANIO_MAXIMO = 2 * 1024 * 1024;

    private static final Set<String> TIPOS_ADMITIDOS = Set.of("image/png", "image/jpeg");

    public LogoDeMarca {
        exigirContenido(contenido);
        exigirTipoAdmitido(tipoMime);
    }

    private static void exigirContenido(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new BusinessRuleException("El logo está vacío");
        }
        if (contenido.length > TAMANIO_MAXIMO) {
            throw new BusinessRuleException("El logo pesa más de 2 MB");
        }
    }

    private static void exigirTipoAdmitido(String tipoMime) {
        if (tipoMime == null || !TIPOS_ADMITIDOS.contains(tipoMime)) {
            throw new BusinessRuleException("El logo tiene que ser una imagen PNG o JPG");
        }
    }
}
