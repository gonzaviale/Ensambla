package com.ensambla.backend.domain.mapper.negocio;

import com.ensambla.backend.domain.dto.negocio.ConfiguracionNegocioResponse;
import com.ensambla.backend.domain.dto.negocio.UbicacionNegocioItem;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.UbicacionNegocio;
import com.ensambla.backend.domain.model.zona.Zona;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ActualizarNegocioMapper {

    private final ObtenerConfiguracionNegocioMapper obtenerConfiguracionNegocioMapper;

    public UbicacionNegocio toUbicacion(UbicacionNegocioItem item) {
        if (item == null) {
            return null;
        }
        return new UbicacionNegocio(item.direccion(), item.localidad(), item.latitud(), item.longitud());
    }

    public ConfiguracionNegocioResponse toResponse(Negocio negocio, Zona zona) {
        return obtenerConfiguracionNegocioMapper.toResponse(negocio, zona);
    }
}
