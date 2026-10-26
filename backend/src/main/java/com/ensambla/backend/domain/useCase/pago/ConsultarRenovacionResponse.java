package com.ensambla.backend.domain.useCase.pago;

import com.ensambla.backend.domain.model.pago.RenovacionPremium;

public record ConsultarRenovacionResponse(boolean habilitada, int periodoDias, RenovacionItem contrato) {

    static final int PERIODO_DIAS = 30;

    /** {@code renovacion} es {@code null} cuando no hay contrato. */
    public static ConsultarRenovacionResponse de(boolean habilitada, RenovacionPremium renovacion) {
        return new ConsultarRenovacionResponse(habilitada, PERIODO_DIAS,
                renovacion == null ? null : RenovacionItem.de(renovacion));
    }
}
