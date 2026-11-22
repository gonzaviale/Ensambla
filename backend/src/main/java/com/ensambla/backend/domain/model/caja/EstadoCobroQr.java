package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;

/** Cada estado sabe si admite confirmar o cancelar el cobro. Un cobro vencido se puede confirmar para no perder la venta. */
public enum EstadoCobroQr {

    PENDIENTE(true, true, false, "") {
        @Override
        EstadoCobroQr segunVencimiento(boolean vencio) {
            return vencio ? VENCIDO : this;
        }

        @Override
        EstadoCobroQr sincronizadoCon(EstadoCobroQr informado) {
            return informado;
        }
    },
    VENCIDO(true, true, false, ""),
    COBRADO(false, false, true, "El cobro con QR ya está cobrado") {
        @Override
        EstadoCobroQr sincronizadoCon(EstadoCobroQr informado) {
            return informado == REEMBOLSADO ? REEMBOLSADO : this;
        }
    },
    CANCELADO(false, false, true, "El cobro con QR está cancelado"),
    REEMBOLSADO(false, false, true, "El cobro con QR fue reembolsado");

    private final boolean admiteConfirmacion;
    private final boolean admiteCancelacion;
    private final boolean finalizado;
    private final String motivoDeRechazo;

    EstadoCobroQr(boolean admiteConfirmacion, boolean admiteCancelacion, boolean finalizado,
                  String motivoDeRechazo) {
        this.admiteConfirmacion = admiteConfirmacion;
        this.admiteCancelacion = admiteCancelacion;
        this.finalizado = finalizado;
        this.motivoDeRechazo = motivoDeRechazo;
    }

    EstadoCobroQr segunVencimiento(boolean vencio) {
        return this;
    }

    /** Una respuesta anterior nunca puede hacer retroceder un estado final ya conciliado. */
    EstadoCobroQr sincronizadoCon(EstadoCobroQr informado) {
        return this;
    }

    public void exigirQueAdmitaConfirmacion() {
        if (!admiteConfirmacion) {
            throw new BusinessRuleException(motivoDeRechazo);
        }
    }

    public void exigirQueAdmitaCancelacion() {
        if (!admiteCancelacion) {
            throw new BusinessRuleException(motivoDeRechazo);
        }
    }

    public void exigirQueEsteCobrado() {
        if (this != COBRADO) {
            throw new BusinessRuleException("Mercado Pago todavía no acreditó el cobro con QR");
        }
    }

    public boolean estaFinalizado() {
        return finalizado;
    }
}
