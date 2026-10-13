package com.ensambla.backend.domain.dto.inventario;

import java.math.BigDecimal;

/** El movimiento guardado y el stock del producto después de aplicarlo. */
public record RegistrarMovimientoResponse(MovimientoItem movimiento, BigDecimal stockActual) {
}
