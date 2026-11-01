package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.Locale;

public enum DireccionOrden {
    ASC,
    DESC;

    /** Interpreta "asc" o "desc" sin distinguir mayúsculas; vacío significa descendente. */
    public static DireccionOrden desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return DESC;
        }
        return switch (valor.strip().toLowerCase(Locale.ROOT)) {
            case "asc" -> ASC;
            case "desc" -> DESC;
            default -> throw new BusinessRuleException("La dirección del orden tiene que ser asc o desc");
        };
    }
}
