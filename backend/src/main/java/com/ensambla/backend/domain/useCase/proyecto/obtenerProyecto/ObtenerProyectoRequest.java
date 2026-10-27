package com.ensambla.backend.domain.useCase.proyecto.obtenerProyecto;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ObtenerProyectoRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
}

