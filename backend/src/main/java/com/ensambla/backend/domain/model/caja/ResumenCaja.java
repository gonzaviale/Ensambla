package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.model.common.Huella;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

public record ResumenCaja(List<VentaCaja> ventas, BigDecimal total, BigDecimal efectivo,
                          BigDecimal transferencia, BigDecimal tarjeta, OffsetDateTime desde, String revision,
                          List<CobroCaja> cobros) {

    public ResumenCaja {
        cobros = cobros == null ? List.of() : List.copyOf(cobros);
    }

    public static ResumenCaja de(List<VentaCaja> ventas) {
        return de(ventas, List.of());
    }

    public static ResumenCaja de(List<VentaCaja> ventas, List<CobroCaja> cobros) {
        List<VentaCaja> copia = List.copyOf(ventas);
        List<CobroCaja> movimientos = List.copyOf(cobros);
        String revision = copia.stream().map(venta -> venta.id().toString()).sorted().toList().toString();
        if (!movimientos.isEmpty()) {
            revision += ":" + movimientos.stream().map(cobro -> cobro.id().toString()).sorted().toList();
        }
        return new ResumenCaja(copia, copia.stream().map(VentaCaja::total).reduce(BigDecimal.ZERO, BigDecimal::add),
                importe(copia, movimientos, MedioPagoCaja.EFECTIVO),
                importe(copia, movimientos, MedioPagoCaja.TRANSFERENCIA),
                importe(copia, movimientos, MedioPagoCaja.TARJETA),
                Stream.concat(copia.stream().map(VentaCaja::creadoEn), movimientos.stream().map(CobroCaja::creadoEn))
                        .min(OffsetDateTime::compareTo).orElse(null), Huella.de(revision), movimientos);
    }

    private static BigDecimal importe(List<VentaCaja> ventas, List<CobroCaja> cobros, MedioPagoCaja medio) {
        BigDecimal inicial = ventas.stream().flatMap(venta -> venta.pagosIniciales().stream())
                .filter(pago -> pago.medio() == medio).map(PagoCaja::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        return inicial.add(cobros.stream().map(cobro -> cobro.importe(medio)).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
