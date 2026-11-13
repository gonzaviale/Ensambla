package com.ensambla.backend.domain.useCase.pago.consultarRenovacion;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.useCase.pago.ConsultarRenovacionResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConsultarRenovacion {

    private final RenovacionPremiumService renovacionPremiumService;

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    public ConsultarRenovacionResponse execute(UsuarioId usuarioId) {
        return ConsultarRenovacionResponse.de(pasarelaSuscripcionesPort.disponible(),
                renovacionPremiumService.consultar(usuarioId).orElse(null));
    }
}
