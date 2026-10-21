package com.ensambla.backend.domain.model.pago;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PagoRemoto(String facturaId, String pagoId, String contratoId, UUID referencia, BigDecimal importe,
        String moneda, String estado, OffsetDateTime debitoEn, OffsetDateTime modificadoEn) {
}
