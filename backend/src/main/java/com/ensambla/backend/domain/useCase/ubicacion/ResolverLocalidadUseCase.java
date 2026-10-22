package com.ensambla.backend.domain.useCase.ubicacion;

import com.ensambla.backend.domain.dto.ubicacion.ResolverLocalidadRequest;
import com.ensambla.backend.domain.dto.ubicacion.ResolverLocalidadResponse;
import com.ensambla.backend.domain.mapper.ubicacion.ResolverLocalidadMapper;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;

@RequiredArgsConstructor
public class ResolverLocalidadUseCase {

    private final UbicacionService ubicacionService;
    private final ResolverLocalidadMapper resolverLocalidadMapper;
    private final Clock clock;

    public ResolverLocalidadResponse execute(ResolverLocalidadRequest request) {
        return resolverLocalidadMapper.toResponse(
                ubicacionService.resolverLocalidades(request.latitud(), request.longitud(), clock.instant()));
    }
}
