package com.ensambla.backend.domain.useCase.negocio;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.zona.Zona;

/** {@code zona} es sólo el nombre de la zona asignada; nunca datos de otros comercios. */
public record ConfiguracionNegocioResponse(String nombre, RubroNegocio rubro, UbicacionNegocioItem ubicacion,
                                           Boolean comparteDatosZona, String zona) {

    public static ConfiguracionNegocioResponse de(Negocio negocio, Zona zona) {
        return new ConfiguracionNegocioResponse(negocio.getNombre(), negocio.getRubro(),
                UbicacionNegocioItem.de(negocio), negocio.getComparteDatosZona(),
                zona == null ? null : zona.getNombre());
    }
}
