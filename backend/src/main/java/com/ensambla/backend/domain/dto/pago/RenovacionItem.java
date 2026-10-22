package com.ensambla.backend.domain.dto.pago;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RenovacionItem(UUID id, String estado, BigDecimal importe, String moneda,
                             OffsetDateTime primerCobroEn, OffsetDateTime proximoCobroEn,
                             boolean cancelacionSolicitada, String enlace) {
}
