package com.ensambla.backend.domain.useCase.sistema.listarSistemas;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ListarSistemasRequest(UsuarioId usuarioId) {
}
