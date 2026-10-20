package com.ensambla.backend.infrastructure.presentation.mapper.ubicacion;

import com.ensambla.backend.domain.dto.ubicacion.BuscarDireccionesRequest;
import com.ensambla.backend.domain.dto.ubicacion.BuscarDireccionesResponse;
import com.ensambla.backend.domain.dto.ubicacion.ListarRubrosResponse;
import com.ensambla.backend.infrastructure.presentation.dto.ubicacion.BuscarDireccionesWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.ubicacion.DireccionesWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.ubicacion.DireccionesWebResponse.DireccionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.ubicacion.RubrosWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.ubicacion.RubrosWebResponse.RubroResponse;
import org.springframework.stereotype.Component;

@Component
public class UbicacionPresentationMapper {

    public BuscarDireccionesRequest toRequest(BuscarDireccionesWebRequest request) {
        return new BuscarDireccionesRequest(request.consulta());
    }

    public DireccionesWebResponse toResponse(BuscarDireccionesResponse response) {
        return new DireccionesWebResponse(response.resultados().stream()
                .map(item -> new DireccionResponse(item.direccion(), item.latitud(), item.longitud()))
                .toList());
    }

    public RubrosWebResponse toResponse(ListarRubrosResponse response) {
        return new RubrosWebResponse(response.rubros().stream()
                .map(item -> new RubroResponse(item.codigo(), item.nombre()))
                .toList());
    }
}
