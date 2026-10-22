package com.ensambla.backend.domain.useCase.pago;

import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecibirNotificacionPagoUseCase {

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    private final RenovacionPremiumService renovacionPremiumService;

    private final SincronizarRenovacionUseCase sincronizarRenovacionUseCase;

    public void execute(String tipo, String recursoId, String firma, String solicitudId) {
        pasarelaSuscripcionesPort.validarNotificacion(firma, solicitudId, recursoId);
        String contratoId = pasarelaSuscripcionesPort.identificarContrato(tipo, recursoId);
        renovacionPremiumService.buscarPorProveedor(contratoId)
                .ifPresent(renovacion -> sincronizarRenovacionUseCase.execute(renovacion.getId()));
    }
}
