package com.ensambla.backend.domain.useCase.proyecto;

import com.ensambla.backend.domain.dto.proyecto.ObtenerProyectoRequest;
import com.ensambla.backend.domain.dto.proyecto.ProyectoItem;
import com.ensambla.backend.domain.mapper.proyecto.ObtenerProyectoMapper;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerProyectoUseCase {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    private final ObtenerProyectoMapper obtenerProyectoMapper;

    public ProyectoItem execute(ObtenerProyectoRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoService.obtener(negocioId, request.proyectoId());
        return obtenerProyectoMapper.toResponse(proyecto);
    }
}

