package com.ensambla.backend.infrastructure.presentation.dto.inventario;

import java.math.BigDecimal;

/** El movimiento guardado y el stock del producto después de aplicarlo. */
public record MovimientoRegistradoResponse(MovimientoResponse movimiento, BigDecimal stockActual) {
}
