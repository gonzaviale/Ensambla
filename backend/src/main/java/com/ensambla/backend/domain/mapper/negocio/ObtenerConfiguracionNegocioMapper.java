package com.ensambla.backend.domain.mapper.negocio;

import com.ensambla.backend.domain.dto.negocio.ConfiguracionNegocioResponse;
import com.ensambla.backend.domain.dto.negocio.UbicacionNegocioItem;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.zona.Zona;

public class ObtenerConfiguracionNegocioMapper {

    public ConfiguracionNegocioResponse toResponse(Negocio negocio, Zona zona) {
        return new ConfiguracionNegocioResponse(negocio.getNombre(), negocio.getRubro(),
                ubicacion(negocio), negocio.getComparteDatosZona(), zona == null ? null : zona.getNombre());
    }

    private UbicacionNegocioItem ubicacion(Negocio negocio) {
        if (negocio.getLatitud() == null) {
            return null;
        }
        return new UbicacionNegocioItem(negocio.getDireccion(), negocio.getLocalidad(),
                negocio.getLatitud(), negocio.getLongitud());
    }
}
