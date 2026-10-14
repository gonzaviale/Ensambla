package com.ensambla.backend.domain.mapper.receta;

import com.ensambla.backend.domain.dto.receta.RecetaItem;
import com.ensambla.backend.domain.dto.receta.RecetaItem.LineaItem;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;

public class RecetaItemMapper {

    public RecetaItem toItem(RecetaCosteada receta) {
        return new RecetaItem(
                receta.bloqueInsumosId(),
                receta.campoPrecioId(),
                receta.lineas().stream().map(this::toItem).toList(),
                receta.costo(),
                receta.incompleta());
    }

    private LineaItem toItem(RecetaCosteada.Linea linea) {
        return new LineaItem(
                linea.insumoId(),
                linea.insumo(),
                linea.cantidad(),
                linea.unidad(),
                linea.precioUnitario(),
                linea.subtotal());
    }
}
