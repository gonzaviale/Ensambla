package com.ensambla.backend.domain.useCase.proyecto.obtenerProyecto;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerProyecto {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    public ProyectoItem execute(ObtenerProyectoRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoService.obtener(negocioId, request.proyectoId());
        return ProyectoItem.de(proyecto);
    }
}

