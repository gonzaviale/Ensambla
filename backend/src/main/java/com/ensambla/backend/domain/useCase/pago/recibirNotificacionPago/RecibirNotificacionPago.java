package com.ensambla.backend.domain.useCase.pago.recibirNotificacionPago;

import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class RecibirNotificacionPago {

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final RenovacionPremiumService renovacionPremiumService;

    private final Clock clock;

    public void execute(String tipo, String recursoId, String firma, String solicitudId) {
        pasarelaSuscripcionesPort.validarNotificacion(firma, solicitudId, recursoId);
        String contratoId = pasarelaSuscripcionesPort.identificarContrato(tipo, recursoId);
        renovacionPremiumRepository.findByProveedorId(contratoId).ifPresent(renovacion ->
                renovacionPremiumService.sincronizar(renovacion.getId(), OffsetDateTime.now(clock)));
    }
}
