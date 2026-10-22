package com.ensambla.backend.domain.model.pago;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ContratoRemoto(String id, UUID referencia, String enlace, String estado,
        BigDecimal importe, String moneda,
        int frecuencia, String unidad, OffsetDateTime inicioEn, OffsetDateTime proximoCobroEn,
        OffsetDateTime modificadoEn) {
}
