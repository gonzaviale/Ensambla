package com.ensambla.backend.infrastructure.presentation.mapper.plantilla;

import com.ensambla.backend.domain.useCase.plantilla.listarPlantillas.ListarPlantillasResponse;
import com.ensambla.backend.domain.useCase.plantilla.listarPlantillas.PlantillaItem;
import com.ensambla.backend.infrastructure.presentation.dto.plantilla.PlantillaResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlantillaPresentationMapper {

    public List<PlantillaResponse> toResponse(ListarPlantillasResponse response) {
        return response.content().stream().map(this::toResponse).toList();
    }

    private PlantillaResponse toResponse(PlantillaItem plantilla) {
        return new PlantillaResponse(
                plantilla.id(),
                plantilla.nombre(),
                plantilla.descripcion(),
                plantilla.icono(),
                plantilla.estructura());
    }
}
