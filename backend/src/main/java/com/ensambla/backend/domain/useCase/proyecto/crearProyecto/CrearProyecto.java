package com.ensambla.backend.domain.useCase.proyecto.crearProyecto;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CrearProyecto {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    private final Clock clock;

    public ProyectoItem execute(CrearProyectoRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoService.crear(
                negocioId, request.nombre(), request.descripcion(), OffsetDateTime.now(clock));
        return ProyectoItem.de(proyecto);
    }
}

