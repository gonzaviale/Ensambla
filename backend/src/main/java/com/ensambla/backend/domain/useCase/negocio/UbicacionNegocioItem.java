package com.ensambla.backend.domain.useCase.negocio;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.UbicacionNegocio;

import java.math.BigDecimal;

public record UbicacionNegocioItem(String direccion, String localidad, BigDecimal latitud, BigDecimal longitud) {

    /** {@code null} si el negocio todavía no tiene ubicación. */
    static UbicacionNegocioItem de(Negocio negocio) {
        if (negocio.getLatitud() == null) {
            return null;
        }
        return new UbicacionNegocioItem(negocio.getDireccion(), negocio.getLocalidad(),
                negocio.getLatitud(), negocio.getLongitud());
    }

    public UbicacionNegocio aUbicacionNegocio() {
        return new UbicacionNegocio(direccion, localidad, latitud, longitud);
    }
}
