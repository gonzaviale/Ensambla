package com.ensambla.backend.domain.useCase.negocio;

import com.ensambla.backend.domain.dto.negocio.ActualizarNegocioRequest;
import com.ensambla.backend.domain.dto.negocio.ConfiguracionNegocioResponse;
import com.ensambla.backend.domain.mapper.negocio.ActualizarNegocioMapper;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ActualizarNegocioUseCase {

    private final NegocioService negocioService;
    private final ActualizarNegocioMapper actualizarNegocioMapper;
    private final Clock clock;

    public ConfiguracionNegocioResponse execute(ActualizarNegocioRequest request) {
        Negocio negocio = negocioService.actualizarConfiguracion(request.usuarioId(), request.nombre(),
                request.rubro(), actualizarNegocioMapper.toUbicacion(request.ubicacion()),
                request.comparteDatosZona(), OffsetDateTime.now(clock));
        return actualizarNegocioMapper.toResponse(negocio);
    }
}
