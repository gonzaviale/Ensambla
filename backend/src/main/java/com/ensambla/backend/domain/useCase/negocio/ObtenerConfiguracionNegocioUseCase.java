package com.ensambla.backend.domain.useCase.negocio;

import com.ensambla.backend.domain.dto.negocio.ConfiguracionNegocioResponse;
import com.ensambla.backend.domain.dto.negocio.ObtenerConfiguracionNegocioRequest;
import com.ensambla.backend.domain.mapper.negocio.ObtenerConfiguracionNegocioMapper;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerConfiguracionNegocioUseCase {

    private final NegocioService negocioService;
    private final ZonaService zonaService;
    private final ObtenerConfiguracionNegocioMapper obtenerConfiguracionNegocioMapper;

    public ConfiguracionNegocioResponse execute(ObtenerConfiguracionNegocioRequest request) {
        Negocio negocio = negocioService.obtenerConfiguracion(request.usuarioId());
        return obtenerConfiguracionNegocioMapper.toResponse(negocio,
                zonaService.obtener(negocio.getZonaId()).orElse(null));
    }
}
