package com.ensambla.backend.domain.useCase.pago.actualizarRenovacion;

import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.useCase.pago.sincronizarRenovacion.SincronizarRenovacion;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ActualizarRenovacion {

    private final NegocioService negocioService;

    private final RenovacionPremiumService renovacionPremiumService;

    private final SincronizarRenovacion sincronizarRenovacion;

    public void execute(UUID usuarioId) {
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        renovacionPremiumService.consultar(negocioId)
                .ifPresent(renovacion -> sincronizarRenovacion.execute(renovacion.getId()));
    }
}
