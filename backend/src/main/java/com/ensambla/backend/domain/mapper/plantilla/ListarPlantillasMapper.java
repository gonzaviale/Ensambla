package com.ensambla.backend.domain.mapper.plantilla;

import com.ensambla.backend.domain.dto.plantilla.ListarPlantillasResponse;
import com.ensambla.backend.domain.dto.plantilla.PlantillaItem;
import com.ensambla.backend.domain.model.plantilla.Plantilla;

import java.util.List;

public class ListarPlantillasMapper {

    public ListarPlantillasResponse toResponse(List<Plantilla> plantillas) {
        return new ListarPlantillasResponse(plantillas.stream().map(this::toItem).toList());
    }

    private PlantillaItem toItem(Plantilla plantilla) {
        return new PlantillaItem(
                plantilla.getId(),
                plantilla.getNombre(),
                plantilla.getDescripcion(),
                plantilla.getIcono(),
                plantilla.getEstructura());
    }
}
