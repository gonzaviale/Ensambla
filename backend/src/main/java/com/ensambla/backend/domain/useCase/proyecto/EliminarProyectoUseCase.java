package com.ensambla.backend.domain.useCase.proyecto;

import com.ensambla.backend.domain.dto.proyecto.EliminarProyectoRequest;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class EliminarProyectoUseCase {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    public void execute(EliminarProyectoRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        proyectoService.eliminar(negocioId, request.proyectoId());
    }
}

