package com.ensambla.backend.domain.useCase.pago.recibirNotificacionPago;

import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.useCase.pago.sincronizarRenovacion.SincronizarRenovacion;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecibirNotificacionPago {

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final SincronizarRenovacion sincronizarRenovacion;

    public void execute(String tipo, String recursoId, String firma, String solicitudId) {
        pasarelaSuscripcionesPort.validarNotificacion(firma, solicitudId, recursoId);
        String contratoId = pasarelaSuscripcionesPort.identificarContrato(tipo, recursoId);
        renovacionPremiumRepository.findByProveedorId(contratoId)
                .ifPresent(renovacion -> sincronizarRenovacion.execute(renovacion.getId()));
    }
}
