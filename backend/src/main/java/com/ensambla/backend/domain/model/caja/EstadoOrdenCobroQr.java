package com.ensambla.backend.domain.model.caja;

/** Estado relevante de una orden QR informado por la pasarela. */
public enum EstadoOrdenCobroQr {
    PENDIENTE(EstadoCobroQr.PENDIENTE),
    COBRADO(EstadoCobroQr.COBRADO),
    CANCELADO(EstadoCobroQr.CANCELADO),
    VENCIDO(EstadoCobroQr.VENCIDO),
    REEMBOLSADO(EstadoCobroQr.REEMBOLSADO);

    private final EstadoCobroQr estadoDelCobro;

    EstadoOrdenCobroQr(EstadoCobroQr estadoDelCobro) {
        this.estadoDelCobro = estadoDelCobro;
    }

    public EstadoCobroQr estadoDelCobro() {
        return estadoDelCobro;
    }
}
