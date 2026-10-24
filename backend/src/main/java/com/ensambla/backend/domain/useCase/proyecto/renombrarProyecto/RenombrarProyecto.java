package com.ensambla.backend.domain.useCase.proyecto.renombrarProyecto;

import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class RenombrarProyecto {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    private final Clock clock;

    public ProyectoItem execute(RenombrarProyectoRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoService.renombrar(
                negocioId, request.proyectoId(), request.nombre(), OffsetDateTime.now(clock));
        return ProyectoItem.de(proyecto);
    }
}

