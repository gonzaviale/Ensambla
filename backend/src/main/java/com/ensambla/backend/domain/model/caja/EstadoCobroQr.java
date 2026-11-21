package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;

/** Cada estado sabe si admite confirmar o cancelar el cobro. Un cobro vencido se puede confirmar para no perder la venta. */
public enum EstadoCobroQr {

    PENDIENTE(true, true, "") {
        @Override
        EstadoCobroQr segunVencimiento(boolean vencio) {
            return vencio ? VENCIDO : this;
        }
    },
    VENCIDO(true, true, ""),
    COBRADO(false, false, "El cobro con QR ya está cobrado"),
    CANCELADO(false, false, "El cobro con QR está cancelado");

    private final boolean admiteConfirmacion;
    private final boolean admiteCancelacion;
    private final String motivoDeRechazo;

    EstadoCobroQr(boolean admiteConfirmacion, boolean admiteCancelacion, String motivoDeRechazo) {
        this.admiteConfirmacion = admiteConfirmacion;
        this.admiteCancelacion = admiteCancelacion;
        this.motivoDeRechazo = motivoDeRechazo;
    }

    EstadoCobroQr segunVencimiento(boolean vencio) {
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
}
