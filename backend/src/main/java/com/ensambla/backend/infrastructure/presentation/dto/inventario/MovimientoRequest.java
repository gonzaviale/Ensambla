package com.ensambla.backend.infrastructure.presentation.dto.inventario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Body de registrar un movimiento. El producto, el tipo (ENTRADA o SALIDA), la cantidad, la fecha y la nota los valida
 * el dominio, que responde 400 con un mensaje por dato. Sin fecha, es de hoy.
 */
public record MovimientoRequest(
        UUID registroId,
        String tipo,
        BigDecimal cantidad,
        LocalDate fecha,
        String nota
) {
}
