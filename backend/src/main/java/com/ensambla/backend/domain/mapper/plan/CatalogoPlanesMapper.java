package com.ensambla.backend.domain.mapper.plan;

import com.ensambla.backend.domain.dto.plan.PlanItem;
import com.ensambla.backend.domain.model.plan.Plan;

import java.util.List;

public class CatalogoPlanesMapper {

    public List<PlanItem> toResponse(List<Plan> planes) {
        return planes.stream().map(this::toItem).toList();
    }

    private PlanItem toItem(Plan plan) {
        return new PlanItem(plan.getCodigo().name(), plan.getNombre(), Boolean.TRUE.equals(plan.getConfigurado()),
                Boolean.TRUE.equals(plan.getLimitesConfigurados()), plan.getPrecioMensual(), plan.getMoneda(),
                plan.getLimiteProyectos(), plan.getLimiteRegistros(), plan.getLimitePaginasIaMensuales(),
                plan.getFunciones());
    }
}
