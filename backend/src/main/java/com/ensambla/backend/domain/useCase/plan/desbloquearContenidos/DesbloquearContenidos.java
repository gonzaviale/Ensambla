package com.ensambla.backend.domain.useCase.plan.desbloquearContenidos;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class DesbloquearContenidos {

    private final NegocioRepository negocioRepository;

    private final ControlPlanService controlPlanService;

    private final Clock clock;

    public AccesoContenidoItem execute(DesbloquearContenidosRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return seleccionarContenidos(negocioId, request);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private AccesoContenidoItem seleccionarContenidos(NegocioId negocioId, DesbloquearContenidosRequest request) {
        return controlPlanService.seleccionar(negocioId, TipoContenidoPlan.valueOf(request.tipo()),
                request.claves(), OffsetDateTime.now(clock));
    }
}
