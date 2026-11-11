package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse.PasoItem;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse.PrecioZonaItem;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.PasoObjetivoWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.PasoObjetivoWebResponse.PrecioZonaWebResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ObjetivoPresentationMapper {

    public ObtenerPasosRequest toObtenerPasosRequest(UsuarioId usuarioId) {
        return new ObtenerPasosRequest(usuarioId);
    }

    public List<PasoObjetivoWebResponse> toResponse(ObtenerPasosResponse response) {
        return response.content().stream().map(this::toResponse).toList();
    }

    private PasoObjetivoWebResponse toResponse(PasoItem paso) {
        return new PasoObjetivoWebResponse(
                paso.tipo(),
                paso.nombre(),
                paso.precio(),
                paso.costo(),
                paso.stock(),
                paso.vendidas30Dias(),
                toResponse(paso.precioZona()));
    }

    private PrecioZonaWebResponse toResponse(PrecioZonaItem precioZona) {
        return precioZona == null
                ? null
                : new PrecioZonaWebResponse(precioZona.minimo(), precioZona.promedio(), precioZona.maximo());
    }
}
