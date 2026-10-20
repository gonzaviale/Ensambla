package com.ensambla.backend.domain.mapper.ubicacion;

import com.ensambla.backend.domain.dto.ubicacion.BuscarDireccionesResponse;
import com.ensambla.backend.domain.dto.ubicacion.BuscarDireccionesResponse.DireccionItem;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;

import java.util.List;

public class BuscarDireccionesMapper {

    public BuscarDireccionesResponse toResponse(List<DireccionEncontrada> direcciones) {
        return new BuscarDireccionesResponse(direcciones.stream()
                .map(direccion -> new DireccionItem(
                        direccion.direccion(),
                        direccion.latitud(),
                        direccion.longitud()))
                .toList());
    }
}
