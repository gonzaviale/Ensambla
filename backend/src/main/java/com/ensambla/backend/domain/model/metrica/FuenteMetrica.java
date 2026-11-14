package com.ensambla.backend.domain.model.metrica;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** De qué tabla del cuaderno sale una métrica. */
@Getter
@RequiredArgsConstructor
public enum FuenteMetrica {

    VENTAS("la tabla de ventas", "ventas"),
    GASTOS("la tabla de gastos", "gastos"),
    INVENTARIO("la tabla de inventario", "productos");

    /** Cómo se nombra en una frase: "de la tabla de ventas". */
    private final String tabla;

    /** Cómo se nombra a lo que se cuenta: "no hay ventas en este período". */
    private final String filas;

    public boolean tieneFecha() {
        return this != INVENTARIO;
    }
}
