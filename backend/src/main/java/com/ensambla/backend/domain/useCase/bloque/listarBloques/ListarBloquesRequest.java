package com.ensambla.backend.domain.useCase.bloque.listarBloques;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ListarBloquesRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
}
