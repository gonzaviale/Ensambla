package com.ensambla.backend.domain.useCase.proyecto.renombrarProyecto;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record RenombrarProyectoRequest(UsuarioId usuarioId, ProyectoId proyectoId, String nombre) {
}

