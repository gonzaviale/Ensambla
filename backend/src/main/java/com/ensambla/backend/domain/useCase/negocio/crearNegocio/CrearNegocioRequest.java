package com.ensambla.backend.domain.useCase.negocio.crearNegocio;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.negocio.UbicacionNegocioItem;

import java.util.Objects;

public record CrearNegocioRequest(
        UsuarioId usuarioId, String nombre, RubroNegocio rubro, UbicacionNegocioItem ubicacion) {

    public CrearNegocioRequest {
        Objects.requireNonNull(usuarioId, "El usuario es obligatorio");
    }
}
