package com.ensambla.backend.domain.useCase.plan.desbloquearContenidos;

import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class DesbloquearContenidos {

    private final NegocioService negocioService;

    private final AccesoContenidoService accesoContenidoService;

    private final Clock clock;

    public AccesoContenidoItem execute(DesbloquearContenidosRequest request) {
        return accesoContenidoService.seleccionar(negocioService.obtenerDelUsuario(request.usuarioId()),
                TipoContenidoPlan.valueOf(request.tipo()), request.claves(), OffsetDateTime.now(clock));
    }
}
