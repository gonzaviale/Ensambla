package com.ensambla.backend.domain.useCase.proyecto.listarProyectos;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarProyectos {

    private final NegocioService negocioService;

    private final ProyectoRepository proyectoRepository;

    public List<ProyectoItem> execute(ListarProyectosRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        List<Proyecto> proyectos = proyectoRepository.findAllByNegocioId(negocioId);
        return proyectos.stream().map(ProyectoItem::de).toList();
    }
}

