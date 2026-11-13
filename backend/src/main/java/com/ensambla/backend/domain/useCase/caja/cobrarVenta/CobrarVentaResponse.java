package com.ensambla.backend.domain.useCase.caja.cobrarVenta;

import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CobrarVentaResponse(VentaCajaId id, List<LineaItem> lineas, String medio, BigDecimal total,
                                  BigDecimal recibido, BigDecimal vuelto, OffsetDateTime creadoEn) {

    public record LineaItem(RegistroId productoId, String nombre, String modo, BigDecimal cantidad,
                            BigDecimal precio, BigDecimal importe) {
    }

    public static CobrarVentaResponse de(VentaCaja venta) {
        return new CobrarVentaResponse(venta.id(), venta.lineas().stream().map(linea -> new LineaItem(
                linea.productoId(), linea.nombre(), linea.modo().name(), linea.cantidad(),
                linea.precio(), linea.importe())).toList(), venta.medio().name(), venta.total(),
                venta.recibido(), venta.vuelto(), venta.creadoEn());
    }
}
