package com.ensambla.backend.domain.mapper.pago;

import com.ensambla.backend.domain.dto.pago.ConsultarRenovacionResponse;
import com.ensambla.backend.domain.dto.pago.RenovacionItem;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;

public class ConsultarRenovacionMapper {

    public ConsultarRenovacionResponse toResponse(boolean habilitada, RenovacionPremium renovacion) {
        RenovacionItem contrato = renovacion == null ? null : new RenovacionItem(
                renovacion.getId(), renovacion.getEstado(), renovacion.getImporte(), renovacion.getMoneda(),
                renovacion.getPrimerCobroEn(), renovacion.getProximoCobroEn(), renovacion.isCancelacionSolicitada(),
                renovacion.getEnlace());
        return new ConsultarRenovacionResponse(habilitada, 30, contrato);
    }
}
