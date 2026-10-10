package com.ensambla.backend.domain.dto.plantilla;

import java.util.Map;
import java.util.UUID;

public record PlantillaItem(
        UUID id,
        String nombre,
        String descripcion,
        String icono,
        Map<String, Object> estructura
) {
}
