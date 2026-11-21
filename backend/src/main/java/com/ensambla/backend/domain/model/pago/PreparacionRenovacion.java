package com.ensambla.backend.domain.model.pago;

public record PreparacionRenovacion(RenovacionPremium renovacion, boolean nueva) {

    public static PreparacionRenovacion reutilizada(RenovacionPremium renovacion) {
        return new PreparacionRenovacion(renovacion, false);
    }

    public static PreparacionRenovacion nueva(RenovacionPremium renovacion) {
        return new PreparacionRenovacion(renovacion, true);
    }
}
