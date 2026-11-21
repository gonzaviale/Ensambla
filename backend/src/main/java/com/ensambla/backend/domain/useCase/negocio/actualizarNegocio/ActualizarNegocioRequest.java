package com.ensambla.backend.domain.useCase.negocio.actualizarNegocio;

import com.ensambla.backend.domain.model.negocio.DestinoDeCobro;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.negocio.UbicacionNegocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.negocio.UbicacionNegocioItem;

public record ActualizarNegocioRequest(UsuarioId usuarioId, String nombre, RubroNegocio rubro,
                                       UbicacionNegocioItem ubicacion, Boolean comparteDatosZona,
                                       String cobroAlias, String cobroTitular) {

    /** {@code null} si no mandó ubicación. */
    public UbicacionNegocio ubicacionNegocio() {
        return ubicacion == null ? null : ubicacion.aUbicacionNegocio();
    }

    /** {@code null} si no mandó alias ni titular; si manda alguno, reemplaza los dos. */
    public DestinoDeCobro destinoDeCobro() {
        if (cobroAlias == null && cobroTitular == null) {
            return null;
        }
        return DestinoDeCobro.de(cobroAlias, cobroTitular);
    }
}
