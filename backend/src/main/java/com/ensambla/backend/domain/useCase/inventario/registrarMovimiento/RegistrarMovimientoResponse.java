package com.ensambla.backend.domain.useCase.inventario.registrarMovimiento;

import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.useCase.inventario.MovimientoItem;

import java.math.BigDecimal;

/** El movimiento guardado y el stock del producto después de aplicarlo. */
public record RegistrarMovimientoResponse(MovimientoItem movimiento, BigDecimal stockActual) {

    public static RegistrarMovimientoResponse de(MovimientoStock movimiento, String producto, BigDecimal stock) {
        return new RegistrarMovimientoResponse(MovimientoItem.de(movimiento, producto), stock);
    }
}
