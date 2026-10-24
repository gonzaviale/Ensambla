package com.ensambla.backend.domain.useCase.proyecto.eliminarProyecto;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record EliminarProyectoRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
}

