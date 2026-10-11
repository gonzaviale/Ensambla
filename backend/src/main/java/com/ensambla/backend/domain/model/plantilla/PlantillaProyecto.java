package com.ensambla.backend.domain.model.plantilla;

import java.util.UUID;

public record PlantillaProyecto(UUID id, String nombre, String descripcion, EstructuraPlantilla estructura) {
}

