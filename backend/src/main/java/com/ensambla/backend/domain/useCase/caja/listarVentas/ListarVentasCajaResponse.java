package com.ensambla.backend.domain.useCase.caja.listarVentas;

import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.domain.useCase.caja.CobroCajaItem;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaResponse;

import java.util.List;

public record ListarVentasCajaResponse(List<CobrarVentaResponse> ventas, List<CobroCajaItem> cobros) {

    public static ListarVentasCajaResponse de(List<VentaCaja> ventas, List<CobroCaja> cobros) {
        return new ListarVentasCajaResponse(ventas.stream().map(CobrarVentaResponse::de).toList(),
                cobros.stream().map(CobroCajaItem::de).toList());
    }
}
