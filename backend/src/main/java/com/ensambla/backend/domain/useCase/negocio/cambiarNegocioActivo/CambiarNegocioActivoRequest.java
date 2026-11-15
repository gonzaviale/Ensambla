package com.ensambla.backend.domain.useCase.negocio.cambiarNegocioActivo;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.Objects;

public record CambiarNegocioActivoRequest(UsuarioId usuarioId, NegocioId negocioId) {

    public CambiarNegocioActivoRequest {
        Objects.requireNonNull(usuarioId, "El usuario es obligatorio");
        Objects.requireNonNull(negocioId, "El negocio es obligatorio");
    }
}
