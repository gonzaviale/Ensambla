package com.ensambla.backend.domain.useCase.pago.cancelarRenovacion;

import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.useCase.pago.ConsultarRenovacionResponse;
import com.ensambla.backend.domain.useCase.pago.sincronizarRenovacion.SincronizarRenovacion;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class CancelarRenovacion {

    private final NegocioService negocioService;

    private final RenovacionPremiumService renovacionPremiumService;

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    private final SincronizarRenovacion sincronizarRenovacion;

    private final Clock clock;

    public ConsultarRenovacionResponse execute(UUID usuarioId) {
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        RenovacionPremium renovacion = renovacionPremiumService.solicitarCancelacion(negocioId,
                OffsetDateTime.now(clock));
        sincronizarRenovacion.execute(renovacion.getId());
        return ConsultarRenovacionResponse.de(pasarelaSuscripcionesPort.disponible(),
                renovacionPremiumService.consultar(negocioId).orElseThrow());
    }
}
