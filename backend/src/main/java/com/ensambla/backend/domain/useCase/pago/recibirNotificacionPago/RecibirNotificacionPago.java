package com.ensambla.backend.domain.useCase.pago.recibirNotificacionPago;

import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@RequiredArgsConstructor
public class RecibirNotificacionPago {

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final RenovacionPremiumService renovacionPremiumService;

    private final Clock clock;

    public void execute(String tipo, String recursoId, String firma, String solicitudId) {
        validarNotificacion(firma, solicitudId, recursoId);
        String contratoId = identificarContrato(tipo, recursoId);
        buscarRenovacionDelContrato(contratoId).ifPresent(this::sincronizar);
    }

    private void validarNotificacion(String firma, String solicitudId, String recursoId) {
        pasarelaSuscripcionesPort.validarNotificacion(firma, solicitudId, recursoId);
    }

    private String identificarContrato(String tipo, String recursoId) {
        return pasarelaSuscripcionesPort.identificarContrato(tipo, recursoId);
    }

    private Optional<RenovacionPremium> buscarRenovacionDelContrato(String contratoId) {
        return renovacionPremiumRepository.findByProveedorId(contratoId);
    }

    private void sincronizar(RenovacionPremium renovacion) {
        renovacionPremiumService.sincronizar(renovacion.getId(), OffsetDateTime.now(clock));
    }
}
