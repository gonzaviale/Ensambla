package com.ensambla.backend.domain.useCase.negocio;

import com.ensambla.backend.domain.dto.negocio.ConfiguracionNegocioResponse;
import com.ensambla.backend.domain.dto.negocio.ObtenerConfiguracionNegocioRequest;
import com.ensambla.backend.domain.mapper.negocio.ObtenerConfiguracionNegocioMapper;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerConfiguracionNegocioUseCase {

    private final NegocioService negocioService;
    private final ObtenerConfiguracionNegocioMapper obtenerConfiguracionNegocioMapper;

    public ConfiguracionNegocioResponse execute(ObtenerConfiguracionNegocioRequest request) {
        return obtenerConfiguracionNegocioMapper.toResponse(negocioService.obtenerConfiguracion(request.usuarioId()));
    }
}
