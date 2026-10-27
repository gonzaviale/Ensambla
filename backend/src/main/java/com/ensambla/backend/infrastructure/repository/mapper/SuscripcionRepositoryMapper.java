package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.PlanId;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.plan.SuscripcionId;
import com.ensambla.backend.infrastructure.repository.entity.SuscripcionEntity;
import org.springframework.stereotype.Component;

@Component
public class SuscripcionRepositoryMapper {

    public Suscripcion toDomain(SuscripcionEntity origen) {
        return Suscripcion.builder()
                .id(Identificador.opcional(origen.getId(), SuscripcionId::de))
                .negocioId(Identificador.opcional(origen.getNegocioId(), NegocioId::de))
                .planId(Identificador.opcional(origen.getPlanId(), PlanId::de))
                .estado(origen.getEstado())
                .inicioEn(origen.getInicioEn())
                .finEn(origen.getFinEn())
                .creadoEn(origen.getCreadoEn())
                .actualizadoEn(origen.getActualizadoEn())
                .build();
    }

    public SuscripcionEntity toEntity(Suscripcion origen) {
        return SuscripcionEntity.builder()
                .id(Identificador.valorDe(origen.getId()))
                .negocioId(Identificador.valorDe(origen.getNegocioId()))
                .planId(Identificador.valorDe(origen.getPlanId()))
                .estado(origen.getEstado())
                .inicioEn(origen.getInicioEn())
                .finEn(origen.getFinEn())
                .creadoEn(origen.getCreadoEn())
                .actualizadoEn(origen.getActualizadoEn())
                .build();
    }
}
