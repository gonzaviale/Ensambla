package com.ensambla.backend.domain.useCase.ubicacion.buscarDirecciones;

import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;

import java.math.BigDecimal;
import java.util.List;

public record BuscarDireccionesResponse(List<DireccionItem> resultados) {

    public record DireccionItem(String direccion, BigDecimal latitud, BigDecimal longitud, String localidad) {
    }

    public static BuscarDireccionesResponse de(List<DireccionEncontrada> direcciones) {
        return new BuscarDireccionesResponse(direcciones.stream()
                .map(direccion -> new DireccionItem(
                        direccion.direccion(),
                        direccion.latitud(),
                        direccion.longitud(),
                        direccion.localidad()))
                .toList());
    }
}
