package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.caja.CierreCaja;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.caja.LineaVentaCaja;
import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.domain.model.caja.ModoVenta;
import com.ensambla.backend.domain.model.caja.PagoCaja;
import com.ensambla.backend.domain.model.caja.ResumenCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCajaRequest;

import java.math.BigDecimal;
import java.util.List;

public final class CierreCajaTestData {

    public static final CierreCajaId CIERRE = CierreCajaId.nuevo();

    private CierreCajaTestData() {
    }

    public static VentaCaja venta() {
        return VentaCaja.nueva(CajaTestData.OPERACION, CajaTestData.NEGOCIO, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, "huella", List.of(LineaVentaCaja.de(CajaTestData.PRODUCTO,
                        "Pan", ModoVenta.PESO, new BigDecimal("0.5"), new BigDecimal("4000"))),
                MedioPagoCaja.MIXTO, new BigDecimal("1500"), CajaTestData.AHORA,
                List.of(new PagoCaja(MedioPagoCaja.EFECTIVO, new BigDecimal("1000")),
                        new PagoCaja(MedioPagoCaja.TRANSFERENCIA, new BigDecimal("700")),
                        new PagoCaja(MedioPagoCaja.TARJETA, new BigDecimal("300"))));
    }

    public static CerrarCajaRequest request() {
        return new CerrarCajaRequest(CajaTestData.USUARIO, CIERRE, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, ResumenCaja.de(List.of(venta())).revision(),
                new BigDecimal("500"), new BigDecimal("100"), new BigDecimal("200"),
                new BigDecimal("1400"), "Revisado");
    }

    public static CierreCaja cierre() {
        CerrarCajaRequest request = request();
        return CierreCaja.nuevo(CIERRE, CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO,
                request.huella(), ResumenCaja.de(List.of(venta())), request.revision(), request.fondoInicial(),
                request.entradas(), request.retiros(), request.contado(), request.nota(), CajaTestData.AHORA);
    }
}
