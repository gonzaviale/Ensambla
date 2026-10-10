package com.ensambla.backend.domain.mapper.proyecto;

import com.ensambla.backend.domain.dto.proyecto.ProyectoItem;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class ObtenerProyectoMapper {

    private final ProyectoItemMapper proyectoItemMapper;

    public ProyectoItem toResponse(Proyecto proyecto) {
        return proyectoItemMapper.toResponse(proyecto);
    }
}

