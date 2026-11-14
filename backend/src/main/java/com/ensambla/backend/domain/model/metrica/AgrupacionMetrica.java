package com.ensambla.backend.domain.model.metrica;

import java.util.Set;

/** Por qué se reparte el número: por producto, por cliente, por categoría de gasto o por día de la semana. */
public enum AgrupacionMetrica {

    NINGUNA,
    PRODUCTO,
    CLIENTE,
    CATEGORIA,
    DIA_DE_LA_SEMANA;

    /** Las agrupaciones que tienen sentido para cada tabla: un gasto no tiene producto ni una venta categoría. */
    public static Set<AgrupacionMetrica> paraFuente(FuenteMetrica fuente) {
        return switch (fuente) {
            case VENTAS -> Set.of(NINGUNA, PRODUCTO, CLIENTE, DIA_DE_LA_SEMANA);
            case GASTOS -> Set.of(NINGUNA, CATEGORIA, DIA_DE_LA_SEMANA);
            case INVENTARIO -> Set.of(NINGUNA, PRODUCTO);
        };
    }
}
