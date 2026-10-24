package com.ensambla.backend.domain.useCase.proyecto.listarProyectos;

import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListarProyectos {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    public List<ProyectoItem> execute(ListarProyectosRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        List<Proyecto> proyectos = proyectoService.listar(negocioId);
        return proyectos.stream().map(ProyectoItem::de).toList();
    }
}

