package com.ensambla.backend.domain.useCase.plantilla.listarPlantillas;

import com.ensambla.backend.domain.model.plantilla.Plantilla;

import java.util.Map;
import java.util.UUID;

public record PlantillaItem(
        UUID id,
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
