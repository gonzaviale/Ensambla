package com.ensambla.backend.domain.useCase.bloque.crearBloque;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record CrearBloqueRequest(UsuarioId usuarioId, ProyectoId proyectoId, String nombre) {
}
