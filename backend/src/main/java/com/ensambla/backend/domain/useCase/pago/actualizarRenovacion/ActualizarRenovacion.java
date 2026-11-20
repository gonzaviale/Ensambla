package com.ensambla.backend.domain.useCase.pago.actualizarRenovacion;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@RequiredArgsConstructor
public class ActualizarRenovacion {

    private final NegocioRepository negocioRepository;

    private final RenovacionPremiumService renovacionPremiumService;

    private final Clock clock;

    public void execute(UsuarioId usuarioId) {
        NegocioId negocioId = obtenerNegocioDelUsuario(usuarioId);
        ultimaRenovacionDe(negocioId).ifPresent(this::sincronizar);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Optional<RenovacionPremium> ultimaRenovacionDe(NegocioId negocioId) {
        return renovacionPremiumService.consultar(negocioId);
    }

    private void sincronizar(RenovacionPremium renovacion) {
        renovacionPremiumService.sincronizar(renovacion.getId(), OffsetDateTime.now(clock));
    }
}
