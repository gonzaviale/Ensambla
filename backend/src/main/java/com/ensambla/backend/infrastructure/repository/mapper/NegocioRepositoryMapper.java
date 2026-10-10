package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import org.springframework.stereotype.Component;

@Component
public class NegocioRepositoryMapper {

    public Negocio toDomain(NegocioEntity entity) {
        return Negocio.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .duenioId(entity.getDuenioId())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public NegocioEntity toEntity(Negocio negocio) {
        return NegocioEntity.builder()
                .id(negocio.getId())
                .nombre(negocio.getNombre())
                .duenioId(negocio.getDuenioId())
                .creadoEn(negocio.getCreadoEn())
                .actualizadoEn(negocio.getActualizadoEn())
                .build();
    }
}

