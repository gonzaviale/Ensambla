package com.ensambla.backend.domain.dto.reporte;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Pedido de un reporte de las filas de un bloque (para ver o para descargar). Todos los filtros son opcionales:
 * texto, rango de fechas de carga (inclusive) y un valor de una columna de opción o de sí/no.
 */
public record GenerarReporteRequest(
        UUID usuarioId,
        UUID bloqueId,
        String texto,
        LocalDate desde,
        LocalDate hasta,
        UUID campoId,
        String valor) {
}
