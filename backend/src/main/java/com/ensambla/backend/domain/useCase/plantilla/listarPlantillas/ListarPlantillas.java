package com.ensambla.backend.domain.useCase.plantilla.listarPlantillas;

import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarPlantillas {

    private final PlantillaRepository plantillaRepository;

    public ListarPlantillasResponse execute() {
        return ListarPlantillasResponse.de(obtenerPlantillasActivas());
    }

    private List<Plantilla> obtenerPlantillasActivas() {
        return plantillaRepository.findAllActivas();
    }
}
