package com.ensambla.backend.domain.model.pago;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PagoRemoto(String facturaId, String pagoId, String contratoId, RenovacionId referencia,
        BigDecimal importe,
        String moneda, String estado, OffsetDateTime debitoEn, OffsetDateTime modificadoEn) {
}
