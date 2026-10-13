package com.ensambla.backend.infrastructure.presentation.dto.receta;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Body de guardar la receta: reemplaza la anterior; sin líneas, la quita. La columna del precio (numérica) y cada línea
 * (insumo, cantidad y unidad opcional) los valida el dominio, que responde 400 con un mensaje por dato.
 */
public record RecetaRequest(
        UUID campoPrecioId,
        List<Linea> lineas
) {

    public record Linea(UUID insumoId, BigDecimal cantidad, String unidad) {
    }
}
