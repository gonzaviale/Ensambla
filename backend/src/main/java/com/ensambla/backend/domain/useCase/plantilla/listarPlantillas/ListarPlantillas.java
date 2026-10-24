package com.ensambla.backend.domain.useCase.plantilla.listarPlantillas;

import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListarPlantillas {

    private final PlantillaService plantillaService;

    public ListarPlantillasResponse execute() {
        return ListarPlantillasResponse.de(plantillaService.listarActivas());
    }
}
