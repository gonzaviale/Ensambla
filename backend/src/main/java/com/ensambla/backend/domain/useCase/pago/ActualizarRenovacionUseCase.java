package com.ensambla.backend.domain.useCase.pago;

import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ActualizarRenovacionUseCase {

    private final NegocioService negocioService;

    private final RenovacionPremiumService renovacionPremiumService;

    private final SincronizarRenovacionUseCase sincronizarRenovacionUseCase;

    public void execute(UUID usuarioId) {
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        renovacionPremiumService.consultar(negocioId)
                .ifPresent(renovacion -> sincronizarRenovacionUseCase.execute(renovacion.getId()));
    }
}
