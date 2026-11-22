package com.ensambla.backend.infrastructure.presentation.dto.caja;

import com.ensambla.backend.domain.model.caja.CobroQrId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CobroQrWebResponse(CobroQrId id, BigDecimal importe, String alias, String titular,
                                 String estado, OffsetDateTime venceEn, String qrDinamico,
                                 String qrEstatico, String proveedor) {
}
