package com.ensambla.backend.domain.model.negocio;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RubroNegocio {

    ALMACEN("Almacén"),
    KIOSCO("Kiosco"),
    MINIMERCADO("Minimercado"),
    DIETETICA("Dietética"),
    OTRO("Otro");

    private final String nombre;
}
