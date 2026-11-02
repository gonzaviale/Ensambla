package com.ensambla.backend.domain.useCase.reporte.exportarResumen;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.time.LocalDate;

/**
 * Pedido del resumen en PDF de un sistema (proyecto). Sin fechas son los últimos 30 días; sin {@code hasta} el período
 * termina hoy.
 */
public record ExportarResumenRequest(UsuarioId usuarioId, ProyectoId proyectoId, LocalDate desde, LocalDate hasta) {
}
