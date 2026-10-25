package com.ensambla.backend.domain.useCase.plan;

import com.ensambla.backend.domain.dto.plan.PlanItem;
import com.ensambla.backend.domain.mapper.plan.CatalogoPlanesMapper;
import com.ensambla.backend.domain.service.plan.CatalogoPlanesService;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarCatalogoPlanesUseCase {

    private final CatalogoPlanesService catalogoPlanesService;

    private final CatalogoPlanesMapper catalogoPlanesMapper;

    public List<PlanItem> execute() {
        return catalogoPlanesMapper.toResponse(catalogoPlanesService.listar());
    }
}
