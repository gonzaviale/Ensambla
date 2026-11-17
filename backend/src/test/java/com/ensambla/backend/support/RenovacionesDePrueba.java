package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.ensambla.backend.support.PagoTestData.AHORA;

/** Contratos del proveedor que se apartan en un dato de lo acordado en la renovación. */
public final class RenovacionesDePrueba {

    private RenovacionesDePrueba() {
    }

    public static ContratoRemoto contratoDeOtraRenovacion(ContratoRemoto base) {
        return copia(base, base.id(), RenovacionId.nuevo(), base.estado(), base.importe(), base.moneda(),
                base.frecuencia(), base.unidad(), base.inicioEn());
    }

    public static ContratoRemoto contratoConOtroPrecio(ContratoRemoto base) {
        return copia(base, base.id(), base.referencia(), base.estado(), BigDecimal.ONE, base.moneda(),
                base.frecuencia(), base.unidad(), base.inicioEn());
    }

    public static ContratoRemoto contratoConOtraMoneda(ContratoRemoto base) {
        return copia(base, base.id(), base.referencia(), base.estado(), base.importe(), "USD",
                base.frecuencia(), base.unidad(), base.inicioEn());
    }

    public static ContratoRemoto contratoConOtraFrecuencia(ContratoRemoto base) {
        return copia(base, base.id(), base.referencia(), base.estado(), base.importe(), base.moneda(),
                1, base.unidad(), base.inicioEn());
    }

    public static ContratoRemoto contratoConOtraUnidadDeTiempo(ContratoRemoto base) {
        return copia(base, base.id(), base.referencia(), base.estado(), base.importe(), base.moneda(),
                base.frecuencia(), "months", base.inicioEn());
    }

    public static ContratoRemoto contratoConOtraFechaDeInicio(ContratoRemoto base) {
        return copia(base, base.id(), base.referencia(), base.estado(), base.importe(), base.moneda(),
                base.frecuencia(), base.unidad(), AHORA.minusSeconds(1));
    }

    public static ContratoRemoto contratoDeOtroContratoDelProveedor(ContratoRemoto base) {
        return copia(base, "otro", base.referencia(), base.estado(), base.importe(), base.moneda(),
                base.frecuencia(), base.unidad(), base.inicioEn());
    }

    public static ContratoRemoto contratoConEstadoDesconocido(ContratoRemoto base) {
        return copia(base, base.id(), base.referencia(), "desconocido", base.importe(), base.moneda(),
                base.frecuencia(), base.unidad(), base.inicioEn());
    }

    private static ContratoRemoto copia(ContratoRemoto base, String id, RenovacionId referencia, String estado,
                                        BigDecimal importe, String moneda, int frecuencia, String unidad,
                                        OffsetDateTime inicioEn) {
        return new ContratoRemoto(id, referencia, base.enlace(), estado, importe, moneda, frecuencia, unidad,
                inicioEn, base.proximoCobroEn(), base.modificadoEn());
    }
}
