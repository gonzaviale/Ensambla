package com.ensambla.backend.domain.useCase.pago;

import com.ensambla.backend.domain.dto.pago.ConsultarRenovacionResponse;
import com.ensambla.backend.domain.mapper.pago.ConsultarRenovacionMapper;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarRenovacionUseCase {

    private final NegocioService negocioService;

    private final RenovacionPremiumService renovacionPremiumService;

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    private final ConsultarRenovacionMapper consultarRenovacionMapper;

    public ConsultarRenovacionResponse execute(UUID usuarioId) {
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        return consultarRenovacionMapper.toResponse(pasarelaSuscripcionesPort.disponible(),
                renovacionPremiumService.consultar(negocioId).orElse(null));
    }
}
