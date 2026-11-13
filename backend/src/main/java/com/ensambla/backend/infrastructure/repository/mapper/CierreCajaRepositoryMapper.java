package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.caja.CierreCaja;
import com.ensambla.backend.infrastructure.repository.entity.CierreCajaEntity;
import org.springframework.stereotype.Component;

@Component
public class CierreCajaRepositoryMapper {

    public CierreCaja toDomain(CierreCajaEntity entity) {
        return entity.getDatos();
    }

    public CierreCajaEntity toEntity(CierreCaja cierre) {
        return CierreCajaEntity.builder()
                .id(cierre.id().valor())
                .negocioId(cierre.negocioId().valor())
                .proyectoId(cierre.proyectoId().valor())
                .inventarioId(cierre.inventarioId().valor())
                .datos(cierre)
                .creadoEn(cierre.cerradoEn())
                .actualizadoEn(cierre.cerradoEn())
                .build();
    }
}
