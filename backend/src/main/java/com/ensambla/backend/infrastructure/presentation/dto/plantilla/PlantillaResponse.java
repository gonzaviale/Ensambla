package com.ensambla.backend.infrastructure.presentation.dto.plantilla;

import java.util.Map;
import java.util.UUID;

public record PlantillaResponse(
        UUID id,
        String nombre,
        String descripcion,
        String icono,
        Map<String, Object> estructura
) {
}
