package com.ensambla.backend.domain.useCase.plantilla.listarPlantillas;

import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;

import java.util.Map;

public record PlantillaItem(
        PlantillaId id,
        String nombre,
        String descripcion,
        String icono,
        Map<String, Object> estructura
) {

    static PlantillaItem de(Plantilla plantilla) {
        return new PlantillaItem(
                plantilla.getId(),
                plantilla.getNombre(),
                plantilla.getDescripcion(),
                plantilla.getIcono(),
                plantilla.getEstructura());
    }
}
