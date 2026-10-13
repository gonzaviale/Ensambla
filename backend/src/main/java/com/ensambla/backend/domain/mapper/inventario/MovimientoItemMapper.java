package com.ensambla.backend.domain.mapper.inventario;

import com.ensambla.backend.domain.dto.inventario.MovimientoItem;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;

public class MovimientoItemMapper {

    /** {@code producto} es la etiqueta de la fila movida. */
    public MovimientoItem toItem(MovimientoStock movimiento, String producto) {
        return new MovimientoItem(
                movimiento.getId(),
                movimiento.getRegistroId(),
                producto,
                movimiento.getTipo().name(),
                movimiento.getCantidad(),
                movimiento.getFecha(),
                movimiento.getNota(),
                movimiento.getCreadoEn());
    }
}
