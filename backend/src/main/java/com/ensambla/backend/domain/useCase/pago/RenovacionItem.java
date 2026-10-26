package com.ensambla.backend.domain.useCase.pago;

import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RenovacionItem(RenovacionId id, String estado, BigDecimal importe, String moneda,
                             OffsetDateTime primerCobroEn, OffsetDateTime proximoCobroEn,
                             boolean cancelacionSolicitada, String enlace) {

    static RenovacionItem de(RenovacionPremium renovacion) {
        return new RenovacionItem(
                renovacion.getId(), renovacion.getEstado(), renovacion.getImporte(), renovacion.getMoneda(),
                renovacion.getPrimerCobroEn(), renovacion.getProximoCobroEn(), renovacion.isCancelacionSolicitada(),
                renovacion.getEnlace());
    }
}
