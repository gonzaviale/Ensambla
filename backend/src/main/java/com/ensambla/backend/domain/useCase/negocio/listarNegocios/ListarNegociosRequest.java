package com.ensambla.backend.domain.useCase.negocio.listarNegocios;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.Objects;

public record ListarNegociosRequest(UsuarioId usuarioId) {

    public ListarNegociosRequest {
        Objects.requireNonNull(usuarioId, "El usuario es obligatorio");
    }
}
