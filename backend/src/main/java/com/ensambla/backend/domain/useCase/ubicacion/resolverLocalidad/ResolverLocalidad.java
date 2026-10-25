package com.ensambla.backend.domain.useCase.ubicacion.resolverLocalidad;

import com.ensambla.backend.domain.port.GeocodificadorPort;
import lombok.RequiredArgsConstructor;

import java.time.Clock;

@RequiredArgsConstructor
public class ResolverLocalidad {

    private final GeocodificadorPort geocodificadorPort;
    private final Clock clock;

    public ResolverLocalidadResponse execute(ResolverLocalidadRequest request) {
        return ResolverLocalidadResponse.de(
                geocodificadorPort.resolverLocalidades(request.latitud(), request.longitud(), clock.instant()));
    }
}
