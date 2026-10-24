package com.ensambla.backend.domain.useCase.proyecto.crearProyectoDesdePlantilla;

import java.util.UUID;

public record CrearProyectoDesdePlantillaRequest(UUID usuarioId, UUID plantillaId) {
}

