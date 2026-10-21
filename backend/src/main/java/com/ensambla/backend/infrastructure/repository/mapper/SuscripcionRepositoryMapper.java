package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.infrastructure.repository.entity.SuscripcionEntity;
import org.springframework.stereotype.Component;

@Component
public class SuscripcionRepositoryMapper {

    public Suscripcion toDomain(SuscripcionEntity origen) {
        return Suscripcion.builder()
                .id(origen.getId())
                .negocioId(origen.getNegocioId())
                .planId(origen.getPlanId())
                .estado(origen.getEstado())
                .inicioEn(origen.getInicioEn())
                .finEn(origen.getFinEn())
                .creadoEn(origen.getCreadoEn())
                .actualizadoEn(origen.getActualizadoEn())
                .build();
    }

    public SuscripcionEntity toEntity(Suscripcion origen) {
        return SuscripcionEntity.builder()
                .id(origen.getId())
                .negocioId(origen.getNegocioId())
                .planId(origen.getPlanId())
                .estado(origen.getEstado())
                .inicioEn(origen.getInicioEn())
                .finEn(origen.getFinEn())
                .creadoEn(origen.getCreadoEn())
                .actualizadoEn(origen.getActualizadoEn())
                .build();
    }
}
