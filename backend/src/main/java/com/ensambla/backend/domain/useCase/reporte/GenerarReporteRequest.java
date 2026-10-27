package com.ensambla.backend.domain.useCase.reporte;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.time.LocalDate;

/**
 * Pedido de un reporte de las filas de un bloque (para ver o para descargar). Todos los filtros son opcionales:
 * texto, rango de fechas de carga (inclusive) y un valor de una columna de opción o de sí/no.
 */
public record GenerarReporteRequest(
        UsuarioId usuarioId,
        BloqueId bloqueId,
        String texto,
        LocalDate desde,
        LocalDate hasta,
        CampoId campoId,
        String valor) {
}
