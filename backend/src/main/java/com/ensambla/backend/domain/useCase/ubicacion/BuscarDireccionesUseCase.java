package com.ensambla.backend.domain.useCase.ubicacion;

import com.ensambla.backend.domain.dto.ubicacion.BuscarDireccionesRequest;
import com.ensambla.backend.domain.dto.ubicacion.BuscarDireccionesResponse;
import com.ensambla.backend.domain.mapper.ubicacion.BuscarDireccionesMapper;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.List;

@RequiredArgsConstructor
public class BuscarDireccionesUseCase {

    private final UbicacionService ubicacionService;
    private final BuscarDireccionesMapper buscarDireccionesMapper;
    private final Clock clock;

    public BuscarDireccionesResponse execute(BuscarDireccionesRequest request) {
        List<DireccionEncontrada> direcciones = ubicacionService.buscar(request.consulta(), clock.instant());
        return buscarDireccionesMapper.toResponse(direcciones);
    }
}
