package com.ensambla.backend.domain.useCase.ubicacion.resolverLocalidad;

import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;

@RequiredArgsConstructor
public class ResolverLocalidad {

    private final UbicacionService ubicacionService;
    private final Clock clock;

    public ResolverLocalidadResponse execute(ResolverLocalidadRequest request) {
        return ResolverLocalidadResponse.de(
                ubicacionService.resolverLocalidades(request.latitud(), request.longitud(), clock.instant()));
    }
}
