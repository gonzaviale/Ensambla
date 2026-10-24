package com.ensambla.backend.domain.useCase.plan.iniciarPrueba;

import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.PruebaPremiumService;
import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class IniciarPrueba {

    private final NegocioService negocioService;

    private final PruebaPremiumService pruebaPremiumService;

    private final Clock clock;

    public PruebaPremiumResponse execute(IniciarPruebaRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        PruebaPremium prueba = pruebaPremiumService.iniciar(negocioId, OffsetDateTime.now(clock));
        return PruebaPremiumResponse.de(prueba);
    }
}
