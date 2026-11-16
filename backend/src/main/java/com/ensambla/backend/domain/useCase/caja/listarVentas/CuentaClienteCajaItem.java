package com.ensambla.backend.domain.useCase.caja.listarVentas;

import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.useCase.caja.CobroCajaItem;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

public record CuentaClienteCajaItem(RegistroId clienteId, String clienteNombre, BigDecimal saldo,
        int cantidadPendientes, OffsetDateTime pendienteDesde, List<CobrarVentaResponse> ventas, List<CobroCajaItem> cobros) {

    public static CuentaClienteCajaItem de(List<VentaCaja> ventas, List<CobroCaja> cobros) {
        VentaCaja ultima = ventas.stream().max(Comparator.comparing(VentaCaja::creadoEn)).orElseThrow();
        List<VentaCaja> pendientes = ventas.stream().filter(venta -> venta.saldo().signum() > 0).toList();
        return new CuentaClienteCajaItem(ultima.clienteId(), ultima.clienteNombre(), saldoDe(pendientes),
                pendientes.size(), primeraPendiente(pendientes), ventas.stream().map(CobrarVentaResponse::de).toList(),
                cobros.stream().filter(cobro -> cobro.clienteId().equals(ultima.clienteId())).map(CobroCajaItem::de).toList());
    }

    private static BigDecimal saldoDe(List<VentaCaja> pendientes) {
        return pendientes.stream().map(VentaCaja::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static OffsetDateTime primeraPendiente(List<VentaCaja> pendientes) {
        return pendientes.stream().map(VentaCaja::creadoEn).min(Comparator.naturalOrder()).orElse(null);
    }
}
