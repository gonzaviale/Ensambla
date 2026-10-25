package com.ensambla.backend.infrastructure.presentation.mapper.plan;

import com.ensambla.backend.domain.dto.plan.PlanItem;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PlanesResponse.PlanResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogoPlanesPresentationMapper {

    public List<PlanResponse> toResponse(List<PlanItem> planes) {
        return planes.stream().map(this::toPlan).toList();
    }

    private PlanResponse toPlan(PlanItem plan) {
        return new PlanResponse(plan.codigo(), plan.nombre(), plan.precioConfigurado(), plan.limitesConfigurados(),
                plan.precioMensual(), plan.moneda(), plan.limiteProyectos(), plan.limiteRegistros(),
                plan.limitePaginasIaMensuales(), plan.funciones());
    }
}
