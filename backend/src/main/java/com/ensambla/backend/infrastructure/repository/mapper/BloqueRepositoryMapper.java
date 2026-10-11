package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import org.springframework.stereotype.Component;

@Component
public class BloqueRepositoryMapper {

    public Bloque toDomain(BloqueEntity entity) {
        return Bloque.builder()
                .id(entity.getId())
                .negocioId(entity.getNegocioId())
                .proyectoId(entity.getProyectoId())
                .nombre(entity.getNombre())
                .posicion(entity.getPosicion())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public BloqueEntity toEntity(Bloque bloque) {
        return BloqueEntity.builder()
                .id(bloque.getId())
                .negocioId(bloque.getNegocioId())
                .proyectoId(bloque.getProyectoId())
                .nombre(bloque.getNombre())
                .posicion(bloque.getPosicion())
                .creadoEn(bloque.getCreadoEn())
                .actualizadoEn(bloque.getActualizadoEn())
                .build();
    }
}
