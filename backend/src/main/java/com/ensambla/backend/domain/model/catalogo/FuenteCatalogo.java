package com.ensambla.backend.domain.model.catalogo;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.Arrays;

public enum FuenteCatalogo {
    SEPA("sepa"),
    OPEN_FOOD_FACTS("openfoodfacts");

    private final String clave;

    FuenteCatalogo(String clave) {
        this.clave = clave;
    }

    public static FuenteCatalogo de(String fuente) {
        return Arrays.stream(values())
                .filter(candidata -> candidata.clave.equals(fuente))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("El catálogo indicado no está disponible."));
    }
}
