package com.ensambla.backend.domain.useCase.reporte.obtenerAvisoEconomico;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** Pedido del aviso económico de un sistema (proyecto) del negocio de la sesión. */
public record ObtenerAvisoEconomicoRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
}
