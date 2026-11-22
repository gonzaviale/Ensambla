package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.caja.EstadoOrdenCobroQr;
import com.ensambla.backend.domain.model.caja.OrdenCobroQr;
import com.ensambla.backend.domain.model.negocio.DestinoDeCobro;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;

/** Cobros con QR y destinos de cobro armados en memoria. */
public final class CobrosQrDePrueba {

    public static final CobroQrId COBRO_QR_ID = CobroQrId.de("00000000-0000-4000-8000-0000000000a1");
    public static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-19T15:00:00Z");
    public static final BigDecimal IMPORTE = new BigDecimal("2500.50");
    public static final String ORDEN_ID = "ORD01ENSAMBLA";

    private CobrosQrDePrueba() {
    }

    public static DestinoDeCobro unDestinoDeCobro() {
        return DestinoDeCobro.de("mi.alias.mp", "Ana Pérez");
    }

    public static CobroQr unCobroQrPendiente() {
        return CobroQr.nuevo(COBRO_QR_ID, NEGOCIO_ID, unDestinoDeCobro(), IMPORTE, AHORA);
    }

    public static CobroQr unCobroQrCobrado() {
        return unCobroQrPendiente().confirmarManualmente(AHORA.plusMinutes(5));
    }

    public static CobroQr unCobroQrCancelado() {
        return unCobroQrPendiente().cancelar(AHORA.plusMinutes(1));
    }

    public static OrdenCobroQr unaOrdenQr(EstadoOrdenCobroQr estado) {
        return new OrdenCobroQr(ORDEN_ID, COBRO_QR_ID, IMPORTE, estado, estado.name().toLowerCase(),
                "PAY01ENSAMBLA", "qr-dinamico", "qr-estatico");
    }

    public static CobroQr unCobroQrMercadoPago(EstadoOrdenCobroQr estado) {
        return CobroQr.nuevoMercadoPago(COBRO_QR_ID, NEGOCIO_ID, IMPORTE, unaOrdenQr(estado), AHORA);
    }

    public static OffsetDateTime elMomentoDelVencimiento() {
        return AHORA.plus(CobroQr.VIGENCIA);
    }
}
