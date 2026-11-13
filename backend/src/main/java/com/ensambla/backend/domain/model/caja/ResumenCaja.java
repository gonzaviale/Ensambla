package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.model.common.Huella;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ResumenCaja(List<VentaCaja> ventas, BigDecimal total, BigDecimal efectivo,
                          BigDecimal transferencia, BigDecimal tarjeta, OffsetDateTime desde, String revision) {

    public static ResumenCaja de(List<VentaCaja> ventas) {
        List<VentaCaja> copia = List.copyOf(ventas);
        return new ResumenCaja(copia, copia.stream().map(VentaCaja::total).reduce(BigDecimal.ZERO, BigDecimal::add),
                importe(copia, MedioPagoCaja.EFECTIVO), importe(copia, MedioPagoCaja.TRANSFERENCIA),
                importe(copia, MedioPagoCaja.TARJETA),
                copia.stream().map(VentaCaja::creadoEn).min(OffsetDateTime::compareTo).orElse(null),
                Huella.de(copia.stream().map(venta -> venta.id().toString()).sorted().toList().toString()));
    }

    private static BigDecimal importe(List<VentaCaja> ventas, MedioPagoCaja medio) {
        return ventas.stream().flatMap(venta -> venta.pagos().stream()).filter(pago -> pago.medio() == medio)
                .map(PagoCaja::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
