package com.ensambla.backend.domain.useCase.negocio.eliminarNegocio;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.Objects;

public record EliminarNegocioRequest(UsuarioId usuarioId, NegocioId negocioId) {

    public EliminarNegocioRequest {
        Objects.requireNonNull(usuarioId, "El usuario es obligatorio");
        Objects.requireNonNull(negocioId, "El negocio es obligatorio");
    }
}
