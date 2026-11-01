package com.ensambla.backend.domain.useCase.sistema.eliminarSistema;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record EliminarSistemaRequest(UsuarioId usuarioId, String id) {
}
