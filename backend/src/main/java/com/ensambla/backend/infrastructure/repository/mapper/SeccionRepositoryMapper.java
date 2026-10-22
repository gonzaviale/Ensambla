package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.seccion.Seccion;
import com.ensambla.backend.infrastructure.repository.entity.SeccionEntity;
import org.springframework.stereotype.Component;

@Component
public class SeccionRepositoryMapper {

    public Seccion toDomain(SeccionEntity entity) {
        return Seccion.builder()
                .id(entity.getId())
                .negocioId(entity.getNegocioId())
                .proyectoId(entity.getProyectoId())
                .nombre(entity.getNombre())
                .posicion(entity.getPosicion())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public SeccionEntity toEntity(Seccion seccion) {
        return SeccionEntity.builder()
                .id(seccion.getId())
                .negocioId(seccion.getNegocioId())
                .proyectoId(seccion.getProyectoId())
                .nombre(seccion.getNombre())
                .posicion(seccion.getPosicion())
                .creadoEn(seccion.getCreadoEn())
                .actualizadoEn(seccion.getActualizadoEn())
                .build();
    }
}
