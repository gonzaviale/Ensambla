package com.ensambla.backend.infrastructure.presentation.mapper.pago;

import com.ensambla.backend.domain.useCase.pago.ConsultarRenovacionResponse;
import com.ensambla.backend.domain.useCase.pago.RenovacionItem;
import com.ensambla.backend.infrastructure.presentation.dto.pago.RenovacionResponse;
import org.springframework.stereotype.Component;

@Component
public class PagoPresentationMapper {

    public RenovacionResponse toResponse(ConsultarRenovacionResponse origen) {
        RenovacionItem contrato = origen.contrato();
        return new RenovacionResponse(origen.habilitada(), origen.periodoDias(), contrato == null ? null
                : new RenovacionResponse.ContratoResponse(contrato.id(), contrato.estado(), contrato.importe(),
                        contrato.moneda(), contrato.primerCobroEn(), contrato.proximoCobroEn(),
                        contrato.cancelacionSolicitada(), contrato.enlace()));
    }
}
