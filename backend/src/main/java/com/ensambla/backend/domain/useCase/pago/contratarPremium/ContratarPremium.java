package com.ensambla.backend.domain.useCase.pago.contratarPremium;

import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PreparacionRenovacion;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.service.usuario.UsuarioService;
import com.ensambla.backend.domain.useCase.pago.ConsultarRenovacionResponse;
import com.ensambla.backend.domain.useCase.pago.sincronizarRenovacion.SincronizarRenovacion;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ContratarPremium {

    private final NegocioService negocioService;

    private final UsuarioService usuarioService;

    private final RenovacionPremiumService renovacionPremiumService;

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    private final SincronizarRenovacion sincronizarRenovacion;

    private final Clock clock;

    public ConsultarRenovacionResponse execute(UUID usuarioId) {
        pasarelaSuscripcionesPort.exigirDisponible();
        String email = usuarioService.obtenerActivo(usuarioId).getEmail();
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        PreparacionRenovacion preparacion = renovacionPremiumService.preparar(negocioId, OffsetDateTime.now(clock));
        if (preparacion.nueva()) {
            ContratoRemoto remoto = pasarelaSuscripcionesPort.crear(preparacion.renovacion(), email);
            renovacionPremiumService.actualizarContrato(preparacion.renovacion().getId(), remoto,
                    OffsetDateTime.now(clock));
        } else {
            sincronizarRenovacion.execute(preparacion.renovacion().getId());
        }
        return ConsultarRenovacionResponse.de(pasarelaSuscripcionesPort.disponible(),
                renovacionPremiumService.consultar(negocioId).orElseThrow());
    }
}
