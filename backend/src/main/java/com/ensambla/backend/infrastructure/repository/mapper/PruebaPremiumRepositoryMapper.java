package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.infrastructure.repository.entity.PruebaPremiumEntity;
import org.springframework.stereotype.Component;

@Component
public class PruebaPremiumRepositoryMapper {

    public PruebaPremium toDomain(PruebaPremiumEntity entity) {
        return new PruebaPremium(entity.getNegocioId(), entity.getInicioEn(), entity.getFinEn(), entity.getPaginasIa());
    }

    public PruebaPremiumEntity toEntity(PruebaPremium prueba) {
        return PruebaPremiumEntity.builder()
                .negocioId(prueba.negocioId())
                .inicioEn(prueba.inicioEn())
                .finEn(prueba.finEn())
                .paginasIa(prueba.paginasIa())
                .build();
    }
}
