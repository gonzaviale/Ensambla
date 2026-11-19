package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.Arrays;
import java.util.Locale;

public enum DireccionOrden {
    ASC("asc"),
    DESC("desc");

    private final String texto;

    DireccionOrden(String texto) {
        this.texto = texto;
    }

    /** Interpreta "asc" o "desc" sin distinguir mayúsculas; vacío significa descendente. */
    public static DireccionOrden desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return DESC;
        }
        String buscado = valor.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(direccion -> direccion.texto.equals(buscado))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("La dirección del orden tiene que ser asc o desc"));
    }
}
