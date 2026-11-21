package com.ensambla.backend.domain.useCase.metrica.listarSugeridas;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ListarSugeridasRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
}
