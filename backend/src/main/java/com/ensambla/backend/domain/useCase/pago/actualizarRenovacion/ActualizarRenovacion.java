package com.ensambla.backend.domain.useCase.pago.actualizarRenovacion;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.useCase.pago.sincronizarRenovacion.SincronizarRenovacion;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ActualizarRenovacion {

    private final NegocioService negocioService;

    private final RenovacionPremiumService renovacionPremiumService;

    private final SincronizarRenovacion sincronizarRenovacion;

    public void execute(UsuarioId usuarioId) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(usuarioId);
        renovacionPremiumService.consultar(negocioId)
                .ifPresent(renovacion -> sincronizarRenovacion.execute(renovacion.getId()));
    }
}
