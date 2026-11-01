package com.ensambla.backend.domain.useCase.plantilla.listarPlantillas;

import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListarPlantillas {

    private final PlantillaRepository plantillaRepository;

    public ListarPlantillasResponse execute() {
        return ListarPlantillasResponse.de(plantillaRepository.findAllActivas());
    }
}
