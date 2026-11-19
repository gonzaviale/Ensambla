package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.infrastructure.repository.entity.CobroCajaEntity;
import org.springframework.stereotype.Component;

@Component
public class CobroCajaRepositoryMapper {

    public CobroCaja toDomain(CobroCajaEntity entity) {
        return entity.getDatos();
    }

    public CobroCajaEntity toEntity(CobroCaja cobro) {
        return CobroCajaEntity.builder()
                .id(cobro.id().valor())
                .negocioId(cobro.negocioId().valor())
                .proyectoId(cobro.proyectoId().valor())
                .inventarioId(cobro.inventarioId().valor())
                .datos(cobro)
                .creadoEn(cobro.creadoEn())
                .actualizadoEn(cobro.creadoEn())
                .build();
    }
}
