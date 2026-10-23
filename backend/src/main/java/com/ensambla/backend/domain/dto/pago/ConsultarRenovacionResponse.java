package com.ensambla.backend.domain.dto.pago;

public record ConsultarRenovacionResponse(boolean habilitada, int periodoDias, RenovacionItem contrato) {
}
