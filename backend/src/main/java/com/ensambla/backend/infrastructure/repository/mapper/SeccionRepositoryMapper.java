package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.seccion.Seccion;
import com.ensambla.backend.domain.model.seccion.SeccionId;
import com.ensambla.backend.infrastructure.repository.entity.SeccionEntity;
import org.springframework.stereotype.Component;

@Component
public class SeccionRepositoryMapper {

    public Seccion toDomain(SeccionEntity entity) {
        return Seccion.builder()
                .id(Identificador.opcional(entity.getId(), SeccionId::de))
                .negocioId(Identificador.opcional(entity.getNegocioId(), NegocioId::de))
                .proyectoId(Identificador.opcional(entity.getProyectoId(), ProyectoId::de))
                .nombre(entity.getNombre())
                .posicion(entity.getPosicion())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public SeccionEntity toEntity(Seccion seccion) {
        return SeccionEntity.builder()
                .id(Identificador.valorDe(seccion.getId()))
                .negocioId(Identificador.valorDe(seccion.getNegocioId()))
                .proyectoId(Identificador.valorDe(seccion.getProyectoId()))
                .nombre(seccion.getNombre())
                .posicion(seccion.getPosicion())
                .creadoEn(seccion.getCreadoEn())
                .actualizadoEn(seccion.getActualizadoEn())
                .build();
    }
}
