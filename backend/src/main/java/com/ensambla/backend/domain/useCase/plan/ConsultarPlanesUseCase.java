package com.ensambla.backend.domain.useCase.plan;

import com.ensambla.backend.domain.dto.plan.ConsultarPlanesResponse;
import com.ensambla.backend.domain.mapper.plan.ConsultarPlanesMapper;
import com.ensambla.backend.domain.model.plan.ConsultaPlanes;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ConsultaPlanesService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarPlanesUseCase {

    private final NegocioService negocioService;

    private final ConsultaPlanesService consultaPlanesService;

    private final ConsultarPlanesMapper consultarPlanesMapper;

    private final Clock clock;

    public ConsultarPlanesResponse execute(UUID usuarioId) {
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        ConsultaPlanes consulta = consultaPlanesService.consultar(negocioId, ahora);
        return consultarPlanesMapper.toResponse(consulta, ahora);
    }
}
