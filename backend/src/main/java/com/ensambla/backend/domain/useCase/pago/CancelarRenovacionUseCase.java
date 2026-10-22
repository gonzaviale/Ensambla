package com.ensambla.backend.domain.useCase.pago;

import com.ensambla.backend.domain.dto.pago.ConsultarRenovacionResponse;
import com.ensambla.backend.domain.mapper.pago.ConsultarRenovacionMapper;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class CancelarRenovacionUseCase {

    private final NegocioService negocioService;

    private final RenovacionPremiumService renovacionPremiumService;

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    private final ConsultarRenovacionMapper consultarRenovacionMapper;

    private final SincronizarRenovacionUseCase sincronizarRenovacionUseCase;

    private final Clock clock;

    public ConsultarRenovacionResponse execute(UUID usuarioId) {
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        RenovacionPremium renovacion = renovacionPremiumService.solicitarCancelacion(negocioId,
                OffsetDateTime.now(clock));
        sincronizarRenovacionUseCase.execute(renovacion.getId());
        return consultarRenovacionMapper.toResponse(pasarelaSuscripcionesPort.disponible(),
                renovacionPremiumService.consultar(negocioId).orElseThrow());
    }
}
