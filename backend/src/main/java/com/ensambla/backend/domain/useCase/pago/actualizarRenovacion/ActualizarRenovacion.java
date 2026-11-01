package com.ensambla.backend.domain.useCase.pago.actualizarRenovacion;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ActualizarRenovacion {

    private final NegocioRepository negocioRepository;

    private final RenovacionPremiumService renovacionPremiumService;

    private final Clock clock;

    public void execute(UsuarioId usuarioId) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(usuarioId);
        renovacionPremiumService.consultar(negocioId).ifPresent(renovacion ->
                renovacionPremiumService.sincronizar(renovacion.getId(), OffsetDateTime.now(clock)));
    }
}
