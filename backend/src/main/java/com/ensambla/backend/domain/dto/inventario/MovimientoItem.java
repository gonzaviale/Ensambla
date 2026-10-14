package com.ensambla.backend.domain.dto.inventario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Un movimiento con el nombre del producto (la etiqueta de su fila). */
public record MovimientoItem(
        UUID id,
        UUID registroId,
        String producto,
        String tipo,
        BigDecimal cantidad,
        LocalDate fecha,
        String nota,
        OffsetDateTime creadoEn) {
}
