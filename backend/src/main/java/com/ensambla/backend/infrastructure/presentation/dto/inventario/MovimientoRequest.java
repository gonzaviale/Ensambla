package com.ensambla.backend.infrastructure.presentation.dto.inventario;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body de registrar un movimiento. El producto, el tipo (ENTRADA o SALIDA), la cantidad, la fecha y la nota los valida
 * el dominio, que responde 400 con un mensaje por dato. Sin fecha, es de hoy.
 */
public record MovimientoRequest(
        RegistroId registroId,
        String tipo,
        BigDecimal cantidad,
        LocalDate fecha,
        String nota
) {
}
