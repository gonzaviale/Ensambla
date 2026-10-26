package com.ensambla.backend.domain.useCase.plan.listarCatalogoPlanes;

import com.ensambla.backend.domain.model.plan.Plan;

import java.util.List;

public record ListarCatalogoPlanesResponse(List<PlanCatalogoItem> planes) {

    public static ListarCatalogoPlanesResponse de(List<Plan> planes) {
        return new ListarCatalogoPlanesResponse(planes.stream().map(PlanCatalogoItem::de).toList());
    }
}
