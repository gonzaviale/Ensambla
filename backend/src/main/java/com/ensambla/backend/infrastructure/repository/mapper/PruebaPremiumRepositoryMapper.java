package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.infrastructure.repository.entity.PruebaPremiumEntity;
import org.springframework.stereotype.Component;

@Component
public class PruebaPremiumRepositoryMapper {

    public PruebaPremium toDomain(PruebaPremiumEntity entity) {
        return new PruebaPremium(Identificador.opcional(entity.getUsuarioId(), UsuarioId::de), entity.getInicioEn(),
                entity.getFinEn(), entity.getPaginasIa());
    }

    public PruebaPremiumEntity toEntity(PruebaPremium prueba) {
        return PruebaPremiumEntity.builder()
                .usuarioId(prueba.usuarioId().valor())
                .inicioEn(prueba.inicioEn())
                .finEn(prueba.finEn())
                .paginasIa(prueba.paginasIa())
                .build();
    }
}
