package com.ensambla.backend.infrastructure.presentation.dto.caja;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ResumenCajaWebResponse(List<CobrarVentaWebResponse> ventas, BigDecimal total, BigDecimal efectivo,
                                     BigDecimal transferencia, BigDecimal tarjeta, OffsetDateTime desde,
                                     String revision) {
}
