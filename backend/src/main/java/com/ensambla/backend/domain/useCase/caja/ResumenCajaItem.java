package com.ensambla.backend.domain.useCase.caja;

import com.ensambla.backend.domain.model.caja.ResumenCaja;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ResumenCajaItem(List<CobrarVentaResponse> ventas, BigDecimal total, BigDecimal efectivo,
                              BigDecimal transferencia, BigDecimal tarjeta, OffsetDateTime desde, String revision) {

    public static ResumenCajaItem de(ResumenCaja resumen) {
        return new ResumenCajaItem(resumen.ventas().stream().map(CobrarVentaResponse::de).toList(),
                resumen.total(), resumen.efectivo(), resumen.transferencia(), resumen.tarjeta(),
                resumen.desde(), resumen.revision());
    }
}
