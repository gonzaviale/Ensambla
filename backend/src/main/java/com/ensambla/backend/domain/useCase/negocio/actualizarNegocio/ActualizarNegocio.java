package com.ensambla.backend.domain.useCase.negocio.actualizarNegocio;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import com.ensambla.backend.domain.useCase.negocio.ConfiguracionNegocioResponse;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ActualizarNegocio {

    private final NegocioService negocioService;
    private final ZonaService zonaService;
    private final Clock clock;

    public ConfiguracionNegocioResponse execute(ActualizarNegocioRequest request) {
        Negocio negocio = negocioService.actualizarConfiguracion(request.usuarioId(), request.nombre(),
                request.rubro(), request.ubicacionNegocio(),
                request.comparteDatosZona(), OffsetDateTime.now(clock));
        return ConfiguracionNegocioResponse.de(negocio, zonaService.obtener(negocio.getZonaId()).orElse(null));
    }
}
