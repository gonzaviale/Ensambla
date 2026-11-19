package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.Arrays;
import java.util.Locale;

public enum DireccionOrden {
    ASC("asc"),
    DESC("desc");

    private final String clave;

    DireccionOrden(String clave) {
        this.clave = clave;
    }

    /** Interpreta "asc" o "desc" sin distinguir mayúsculas; vacío significa descendente. */
    public static DireccionOrden desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return DESC;
        }
        return Arrays.stream(values())
                .filter(direccion -> direccion.responde(valor))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("La dirección del orden tiene que ser asc o desc"));
    }

    private boolean responde(String valor) {
        return clave.equals(valor.strip().toLowerCase(Locale.ROOT));
    }
}
