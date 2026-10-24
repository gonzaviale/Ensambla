package com.ensambla.backend.domain.useCase.negocio.obtenerConfiguracionNegocio;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import com.ensambla.backend.domain.useCase.negocio.ConfiguracionNegocioResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerConfiguracionNegocio {

    private final NegocioService negocioService;
    private final ZonaService zonaService;

    public ConfiguracionNegocioResponse execute(ObtenerConfiguracionNegocioRequest request) {
        Negocio negocio = negocioService.obtenerConfiguracion(request.usuarioId());
        return ConfiguracionNegocioResponse.de(negocio,
                zonaService.obtener(negocio.getZonaId()).orElse(null));
    }
}
