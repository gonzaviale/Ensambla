package com.ensambla.backend.domain.useCase.pago.cancelarRenovacion;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.useCase.pago.ConsultarRenovacionResponse;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CancelarRenovacion {

    private final RenovacionPremiumService renovacionPremiumService;

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public ConsultarRenovacionResponse execute(UsuarioId usuarioId) {
        RenovacionPremium renovacion = solicitarCancelacion(usuarioId, OffsetDateTime.now(clock));
        renovacionPremiumService.sincronizar(renovacion.getId(), OffsetDateTime.now(clock));
        return ConsultarRenovacionResponse.de(pasarelaSuscripcionesPort.disponible(),
                renovacionPremiumService.consultar(usuarioId).orElseThrow());
    }

    /** La cancelación se pide acá y se aplica en la pasarela al sincronizar. */
    private RenovacionPremium solicitarCancelacion(UsuarioId usuarioId, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearCuenta(usuarioId);
            RenovacionPremium renovacion = renovacionPremiumService.consultar(usuarioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Renovación no encontrada"));
            renovacion.setCancelacionSolicitada(true);
            renovacion.setActualizadoEn(ahora);
            return renovacionPremiumRepository.save(renovacion);
        });
    }
}
