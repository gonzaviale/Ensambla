package com.ensambla.backend.domain.useCase.pago.actualizarRenovacion;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ActualizarRenovacion {

    private final RenovacionPremiumService renovacionPremiumService;

    private final Clock clock;

    public void execute(UsuarioId usuarioId) {
        renovacionPremiumService.consultar(usuarioId).ifPresent(renovacion ->
                renovacionPremiumService.sincronizar(renovacion.getId(), OffsetDateTime.now(clock)));
    }
}
