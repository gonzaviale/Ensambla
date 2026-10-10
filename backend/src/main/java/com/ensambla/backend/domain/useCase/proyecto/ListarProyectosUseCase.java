package com.ensambla.backend.domain.useCase.proyecto;

import com.ensambla.backend.domain.dto.proyecto.ListarProyectosRequest;
import com.ensambla.backend.domain.dto.proyecto.ProyectoItem;
import com.ensambla.backend.domain.mapper.proyecto.ListarProyectosMapper;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListarProyectosUseCase {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    private final ListarProyectosMapper listarProyectosMapper;

    public List<ProyectoItem> execute(ListarProyectosRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        List<Proyecto> proyectos = proyectoService.listar(negocioId);
        return listarProyectosMapper.toResponse(proyectos);
    }
}

