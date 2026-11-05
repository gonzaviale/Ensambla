package com.ensambla.backend.domain.useCase.reposicion.consultarReposicion;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** Pedido de lo que hay que reponer en un sistema (proyecto) del negocio de la sesión. */
public record ConsultarReposicionRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
}
