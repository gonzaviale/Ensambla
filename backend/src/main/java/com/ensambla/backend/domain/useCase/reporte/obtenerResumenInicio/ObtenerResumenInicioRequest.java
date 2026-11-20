package com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** Pedido de las métricas del inicio de un sistema (proyecto) del negocio de la sesión. */
public record ObtenerResumenInicioRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
}
