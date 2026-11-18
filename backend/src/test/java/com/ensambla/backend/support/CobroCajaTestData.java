package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.domain.model.caja.CobroCajaId;
import com.ensambla.backend.domain.model.caja.LineaVentaCaja;
import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.domain.model.caja.ModoVenta;
import com.ensambla.backend.domain.model.caja.PagoCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.useCase.caja.registrarCobro.RegistrarCobroRequest;

import java.math.BigDecimal;
import java.util.List;

public final class CobroCajaTestData {

    public static final CobroCajaId COBRO = CobroCajaId.nuevo();

    private CobroCajaTestData() {
    }

    public static VentaCaja venta() {
        return venta(CajaTestData.OPERACION, "800");
    }

    public static VentaCaja venta(VentaCajaId id, String pagado) {
        BigDecimal importe = new BigDecimal(pagado);
        List<PagoCaja> pagos = importe.signum() == 0 ? List.of() : List.of(new PagoCaja(MedioPagoCaja.EFECTIVO, importe));
        return VentaCaja.nueva(id, CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO, "venta-" + id,
                List.of(LineaVentaCaja.de(CajaTestData.PRODUCTO, "Pan", ModoVenta.PESO,
                        new BigDecimal("0.5"), new BigDecimal("4000"))), MedioPagoCaja.EFECTIVO, null,
                CajaTestData.AHORA, pagos, CajaTestData.CLIENTE, "Ana López");
    }

    public static RegistrarCobroRequest request() {
        return new RegistrarCobroRequest(CajaTestData.USUARIO, COBRO, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, List.of(new RegistrarCobroRequest.AplicacionRequest(CajaTestData.OPERACION,
                        List.of(new RegistrarCobroRequest.PagoRequest("TRANSFERENCIA", new BigDecimal("600"))))));
    }

    public static CobroCaja cobro() {
        return new CobroCaja(COBRO, CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO,
                CajaTestData.CLIENTE, "Ana López", request().huella(), List.of(new CobroCaja.Aplicacion(
                        CajaTestData.OPERACION, List.of(new PagoCaja(MedioPagoCaja.TRANSFERENCIA, new BigDecimal("600"))))),
                CajaTestData.AHORA.plusDays(1));
    }
}
