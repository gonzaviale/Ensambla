package com.ensambla.backend.domain.useCase.inventario.registrarMovimiento;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Entrada o salida de un producto (un registro del bloque); sin fecha, es de hoy. */
public record RegistrarMovimientoRequest(
        UsuarioId usuarioId,
        BloqueId bloqueId,
        RegistroId registroId,
        String tipo,
        BigDecimal cantidad,
        LocalDate fecha,
        String nota) {
}
