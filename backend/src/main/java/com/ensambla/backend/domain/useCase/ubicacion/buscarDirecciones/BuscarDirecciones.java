package com.ensambla.backend.domain.useCase.ubicacion.buscarDirecciones;

import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.List;

@RequiredArgsConstructor
public class BuscarDirecciones {

    private final UbicacionService ubicacionService;
    private final Clock clock;

    public BuscarDireccionesResponse execute(BuscarDireccionesRequest request) {
        List<DireccionEncontrada> direcciones = ubicacionService.buscar(request.consulta(), clock.instant());
        return BuscarDireccionesResponse.de(direcciones);
    }
}
