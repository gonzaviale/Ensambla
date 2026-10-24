package com.ensambla.backend.infrastructure.presentation.mapper.negocio;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.negocio.ConfiguracionNegocioResponse;
import com.ensambla.backend.domain.useCase.negocio.UbicacionNegocioItem;
import com.ensambla.backend.domain.useCase.negocio.actualizarNegocio.ActualizarNegocioRequest;
import com.ensambla.backend.domain.useCase.negocio.obtenerConfiguracionNegocio.ObtenerConfiguracionNegocioRequest;
import com.ensambla.backend.infrastructure.presentation.dto.negocio.ActualizarNegocioWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.negocio.ConfiguracionNegocioWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.negocio.UbicacionNegocioWeb;
import org.springframework.stereotype.Component;

@Component
public class NegocioPresentationMapper {

    public ObtenerConfiguracionNegocioRequest toObtenerRequest(UsuarioId usuarioId) {
        return new ObtenerConfiguracionNegocioRequest(usuarioId);
    }

    public ActualizarNegocioRequest toActualizarRequest(UsuarioId usuarioId, ActualizarNegocioWebRequest request) {
        UbicacionNegocioWeb punto = request.ubicacion();
        UbicacionNegocioItem ubicacion = punto == null ? null : new UbicacionNegocioItem(
                punto.direccion(), punto.localidad(), punto.latitud(), punto.longitud());
        return new ActualizarNegocioRequest(usuarioId, request.nombre(), request.rubro(),
                ubicacion, request.comparteDatosZona());
    }

    public ConfiguracionNegocioWebResponse toResponse(ConfiguracionNegocioResponse response) {
        UbicacionNegocioItem punto = response.ubicacion();
        UbicacionNegocioWeb ubicacion = punto == null ? null : new UbicacionNegocioWeb(
                punto.direccion(), punto.localidad(), punto.latitud(), punto.longitud());
        return new ConfiguracionNegocioWebResponse(response.nombre(), response.rubro().name(),
                ubicacion, response.comparteDatosZona(), response.zona());
    }
}
