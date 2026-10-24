package com.ensambla.backend.domain.useCase.negocio.actualizarNegocio;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.negocio.UbicacionNegocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.negocio.UbicacionNegocioItem;

public record ActualizarNegocioRequest(UsuarioId usuarioId, String nombre, RubroNegocio rubro,
                                       UbicacionNegocioItem ubicacion, Boolean comparteDatosZona) {

    /** {@code null} si no mandó ubicación. */
    public UbicacionNegocio ubicacionNegocio() {
        return ubicacion == null ? null : ubicacion.aUbicacionNegocio();
    }
}
