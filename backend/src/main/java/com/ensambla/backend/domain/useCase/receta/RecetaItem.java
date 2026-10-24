package com.ensambla.backend.domain.useCase.receta;

import com.ensambla.backend.domain.model.receta.RecetaCosteada;

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

    public static RecetaItem de(RecetaCosteada receta) {
        return new RecetaItem(
                receta.bloqueInsumosId(),
                receta.campoPrecioId(),
                receta.lineas().stream().map(LineaItem::de).toList(),
                receta.costo(),
                receta.incompleta());
    }

    public record LineaItem(
            UUID insumoId,
            String insumo,
            BigDecimal cantidad,
            String unidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal) {

        static LineaItem de(RecetaCosteada.Linea linea) {
            return new LineaItem(
                    linea.insumoId(),
                    linea.insumo(),
                    linea.cantidad(),
                    linea.unidad(),
                    linea.precioUnitario(),
                    linea.subtotal());
        }
    }
}
