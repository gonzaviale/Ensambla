package com.ensambla.backend.domain.useCase.reporte.obtenerMetricas;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.reporte.PeriodoMetricas;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** Pedido de las métricas detalladas de un sistema (proyecto) del negocio de la sesión para un período. */
public record ObtenerMetricasRequest(UsuarioId usuarioId, ProyectoId proyectoId, PeriodoMetricas periodo) {
}
