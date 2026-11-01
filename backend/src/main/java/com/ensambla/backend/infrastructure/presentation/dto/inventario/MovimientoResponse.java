package com.ensambla.backend.infrastructure.presentation.dto.inventario;

import com.ensambla.backend.domain.model.inventario.MovimientoStockId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record MovimientoResponse(
        MovimientoStockId id,
        RegistroId registroId,
        String producto,
        String tipo,
        BigDecimal cantidad,
        LocalDate fecha,
        String nota,
        OffsetDateTime creadoEn
) {
}
