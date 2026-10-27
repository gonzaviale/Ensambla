package com.ensambla.backend.infrastructure.presentation.dto.plantilla;

import com.ensambla.backend.domain.model.plantilla.PlantillaId;

import java.util.Map;

public record PlantillaResponse(
        PlantillaId id,
        String nombre,
        String descripcion,
        String icono,
        Map<String, Object> estructura
) {
}
