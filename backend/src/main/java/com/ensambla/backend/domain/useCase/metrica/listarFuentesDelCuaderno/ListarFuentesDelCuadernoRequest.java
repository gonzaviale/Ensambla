package com.ensambla.backend.domain.useCase.metrica.listarFuentesDelCuaderno;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ListarFuentesDelCuadernoRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
}
