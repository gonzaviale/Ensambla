package com.ensambla.backend.domain.useCase.proyecto;

import com.ensambla.backend.domain.dto.proyecto.CrearProyectoRequest;
import com.ensambla.backend.domain.dto.proyecto.ProyectoItem;
import com.ensambla.backend.domain.mapper.proyecto.CrearProyectoMapper;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearProyectoUseCase {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    private final CrearProyectoMapper crearProyectoMapper;

    private final Clock clock;

    public ProyectoItem execute(CrearProyectoRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoService.crear(
                negocioId, request.nombre(), request.descripcion(), OffsetDateTime.now(clock));
        return crearProyectoMapper.toResponse(proyecto);
    }
}

