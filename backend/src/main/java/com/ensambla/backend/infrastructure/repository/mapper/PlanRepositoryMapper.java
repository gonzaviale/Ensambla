package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PlanId;
import com.ensambla.backend.infrastructure.repository.entity.PlanEntity;
import org.springframework.stereotype.Component;

@Component
public class PlanRepositoryMapper {

    public Plan toDomain(PlanEntity origen) {
        return Plan.builder()
                .id(Identificador.opcional(origen.getId(), PlanId::de))
                .codigo(origen.getCodigo())
                .nombre(origen.getNombre())
                .configurado(origen.getConfigurado())
                .limitesConfigurados(origen.getLimitesConfigurados())
                .precioMensual(origen.getPrecioMensual())
                .moneda(origen.getMoneda())
                .limiteProyectos(origen.getLimiteProyectos())
                .limiteRegistros(origen.getLimiteRegistros())
                .limitePaginasIaMensuales(origen.getLimitePaginasIaMensuales())
                .funciones(origen.getFunciones())
                .creadoEn(origen.getCreadoEn())
                .actualizadoEn(origen.getActualizadoEn())
                .build();
    }
}
