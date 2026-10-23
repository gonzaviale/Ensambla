package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PagoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class PagoTestData {

    public static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-20T12:00:00Z");

    public static final BigDecimal IMPORTE = new BigDecimal("14900.00");

    public static final UUID NEGOCIO_ID = UUID.fromString("b444bcba-a316-4817-951e-a4545d7f1590");

    public static final UUID RENOVACION_ID = UUID.fromString("f4f5a7bb-71c8-43cd-b91e-32a0842b64af");

    public static final String PROVEEDOR_ID = "mp-contrato";

    private PagoTestData() {
    }

    public static RenovacionPremium renovacion() {
        return RenovacionPremium.builder()
                .id(RENOVACION_ID)
                .negocioId(NEGOCIO_ID)
                .proveedorId(PROVEEDOR_ID)
                .enlace("https://www.mercadopago.com.ar/subscriptions/checkout?preapproval_id=" + PROVEEDOR_ID)
                .estado("AUTORIZADA")
                .importe(IMPORTE)
                .moneda("ARS")
                .primerCobroEn(AHORA)
                .proximoCobroEn(AHORA.plusDays(30))
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }

    public static ContratoRemoto contrato(RenovacionPremium renovacion, String estado, OffsetDateTime modificado) {
        return new ContratoRemoto("mp-" + renovacion.getId(), renovacion.getId(),
                "https://www.mercadopago.com.ar/subscriptions/checkout?preapproval_id=mp-" + renovacion.getId(),
                estado, renovacion.getImporte(), renovacion.getMoneda(), 30, "days", renovacion.getPrimerCobroEn(),
                renovacion.getPrimerCobroEn().plusDays(30), modificado);
    }

    public static PagoRemoto pago(RenovacionPremium renovacion, String factura, String estado,
                                  OffsetDateTime fecha, OffsetDateTime modificado) {
        return new PagoRemoto(factura, "pago-" + factura, renovacion.getProveedorId(), renovacion.getId(),
                renovacion.getImporte(), renovacion.getMoneda(), estado, fecha, modificado);
    }
}
