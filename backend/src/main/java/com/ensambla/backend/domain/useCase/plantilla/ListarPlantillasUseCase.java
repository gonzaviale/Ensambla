package com.ensambla.backend.domain.useCase.plantilla;

import com.ensambla.backend.domain.dto.plantilla.ListarPlantillasResponse;
import com.ensambla.backend.domain.mapper.plantilla.ListarPlantillasMapper;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListarPlantillasUseCase {

    private final PlantillaService plantillaService;
    private final ListarPlantillasMapper mapper;

    public ListarPlantillasResponse execute() {
        return mapper.toResponse(plantillaService.listarActivas());
    }
}
