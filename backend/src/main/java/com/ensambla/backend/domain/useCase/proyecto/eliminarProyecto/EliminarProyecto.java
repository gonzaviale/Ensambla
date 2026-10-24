package com.ensambla.backend.domain.useCase.proyecto.eliminarProyecto;

import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class EliminarProyecto {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    public void execute(EliminarProyectoRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        proyectoService.eliminar(negocioId, request.proyectoId());
    }
}

