package com.ensambla.backend.domain.model.catalogo;

import com.ensambla.backend.domain.exception.BusinessRuleException;

public enum FuenteCatalogo {
    SEPA, OPEN_FOOD_FACTS;

    public static FuenteCatalogo de(String fuente) {
        return switch (fuente) {
            case "sepa" -> SEPA;
            case "openfoodfacts" -> OPEN_FOOD_FACTS;
            default -> throw new BusinessRuleException("El catálogo indicado no está disponible.");
        };
    }
}
