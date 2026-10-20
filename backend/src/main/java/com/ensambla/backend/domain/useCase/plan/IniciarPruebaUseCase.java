package com.ensambla.backend.domain.useCase.plan;

import com.ensambla.backend.domain.dto.plan.IniciarPruebaRequest;
import com.ensambla.backend.domain.dto.plan.PruebaPremiumResponse;
import com.ensambla.backend.domain.mapper.plan.IniciarPruebaMapper;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.PruebaPremiumService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class IniciarPruebaUseCase {

    private final NegocioService negocioService;

    private final PruebaPremiumService pruebaPremiumService;

    private final IniciarPruebaMapper iniciarPruebaMapper;

    private final Clock clock;

    public PruebaPremiumResponse execute(IniciarPruebaRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        PruebaPremium prueba = pruebaPremiumService.iniciar(negocioId, OffsetDateTime.now(clock));
        return iniciarPruebaMapper.toResponse(prueba);
    }
}
