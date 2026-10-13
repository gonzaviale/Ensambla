package com.ensambla.backend.domain.dto.receta;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** La receta con su costo (la tabla de los insumos y la columna del precio de la que sale); {@code incompleta} avisa que a algún insumo le falta el precio o ya no existe. */
public record RecetaItem(
        UUID bloqueInsumosId,
        UUID campoPrecioId,
        List<LineaItem> lineas,
        BigDecimal costo,
        boolean incompleta) {

    public record LineaItem(
            UUID insumoId,
            String insumo,
            BigDecimal cantidad,
            String unidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal) {
    }
}
