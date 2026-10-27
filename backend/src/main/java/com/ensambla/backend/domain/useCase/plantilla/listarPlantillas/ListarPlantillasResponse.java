package com.ensambla.backend.domain.useCase.plantilla.listarPlantillas;

import com.ensambla.backend.domain.model.plantilla.Plantilla;

import java.util.List;

public record ListarPlantillasResponse(List<PlantillaItem> content) {

    public static ListarPlantillasResponse de(List<Plantilla> plantillas) {
        return new ListarPlantillasResponse(plantillas.stream().map(PlantillaItem::de).toList());
    }
}
