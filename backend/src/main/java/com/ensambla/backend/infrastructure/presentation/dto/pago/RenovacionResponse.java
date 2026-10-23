package com.ensambla.backend.infrastructure.presentation.dto.pago;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RenovacionResponse(boolean habilitada, int periodoDias, ContratoResponse contrato) {

    public record ContratoResponse(UUID id, String estado, BigDecimal importe, String moneda,
                                   OffsetDateTime primerCobroEn, OffsetDateTime proximoCobroEn,
                                   boolean cancelacionSolicitada, String enlace) {
    }
}
