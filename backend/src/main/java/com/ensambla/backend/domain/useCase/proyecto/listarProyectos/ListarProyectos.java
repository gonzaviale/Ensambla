package com.ensambla.backend.domain.useCase.proyecto.listarProyectos;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarProyectos {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    public List<ProyectoItem> execute(ListarProyectosRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        List<Proyecto> proyectos = proyectoRepository.findAllByNegocioId(negocioId);
        return proyectos.stream().map(ProyectoItem::de).toList();
    }
}

