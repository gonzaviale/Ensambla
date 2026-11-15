package com.ensambla.backend.domain.useCase.caja.listarVentas;

import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.domain.useCase.caja.CobroCajaItem;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaResponse;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public record ListarVentasCajaResponse(List<CobrarVentaResponse> ventas, List<CobroCajaItem> cobros,
                                      List<CuentaClienteCajaItem> cuentas) {

    public static ListarVentasCajaResponse de(List<VentaCaja> ventas, List<CobroCaja> cobros) {
        return new ListarVentasCajaResponse(ventas.stream().map(CobrarVentaResponse::de).toList(),
                cobros.stream().map(CobroCajaItem::de).toList(), cuentasDe(ventas, cobros));
    }
    private static List<CuentaClienteCajaItem> cuentasDe(List<VentaCaja> ventas, List<CobroCaja> cobros) {
        return ventas.stream().filter(venta -> venta.clienteId() != null)
                .collect(Collectors.groupingBy(VentaCaja::clienteId, LinkedHashMap::new, Collectors.toList()))
                .values().stream().map(cuenta -> CuentaClienteCajaItem.de(cuenta, cobros)).toList();
    }
}
