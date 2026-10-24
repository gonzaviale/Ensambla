package com.ensambla.backend.domain.useCase.plan.consultarPlanes;

import com.ensambla.backend.domain.model.plan.ConsultaPlanes;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ConsultaPlanesService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarPlanes {

    private final NegocioService negocioService;

    private final ConsultaPlanesService consultaPlanesService;

    private final Clock clock;

    public ConsultarPlanesResponse execute(UUID usuarioId) {
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        ConsultaPlanes consulta = consultaPlanesService.consultar(negocioId, ahora);
        return ConsultarPlanesResponse.de(consulta, ahora);
    }
}
