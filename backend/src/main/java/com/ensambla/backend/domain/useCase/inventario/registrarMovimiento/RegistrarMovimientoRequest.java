package com.ensambla.backend.domain.useCase.inventario.registrarMovimiento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Entrada o salida de un producto (un registro del bloque); sin fecha, es de hoy. */
public record RegistrarMovimientoRequest(
        UUID usuarioId,
        UUID bloqueId,
        UUID registroId,
        String tipo,
        BigDecimal cantidad,
        LocalDate fecha,
        String nota) {
}
