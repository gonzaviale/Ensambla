package com.ensambla.backend.domain.useCase.proyecto.obtenerProyecto;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerProyecto {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    public ProyectoItem execute(ObtenerProyectoRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoRepository.obtener(negocioId, request.proyectoId());
        return ProyectoItem.de(proyecto);
    }
}

