package com.ensambla.backend.domain.useCase.proyecto.crearProyecto;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record CrearProyectoRequest(UsuarioId usuarioId, String nombre, String descripcion) {
}

