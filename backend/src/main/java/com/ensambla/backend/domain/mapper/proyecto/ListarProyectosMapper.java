package com.ensambla.backend.domain.mapper.proyecto;

import com.ensambla.backend.domain.dto.proyecto.ProyectoItem;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import lombok.RequiredArgsConstructor;

import java.util.List;


@RequiredArgsConstructor
public class ListarProyectosMapper {

    private final ProyectoItemMapper proyectoItemMapper;

    public List<ProyectoItem> toResponse(List<Proyecto> proyectos) {
        return proyectos.stream().map(proyectoItemMapper::toResponse).toList();
    }
}

