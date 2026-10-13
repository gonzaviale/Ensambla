package com.ensambla.backend.infrastructure.presentation.dto.inventario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MovimientoResponse(
        UUID id,
        UUID registroId,
        String producto,
        String tipo,
        BigDecimal cantidad,
        LocalDate fecha,
        String nota,
        OffsetDateTime creadoEn
) {
}
