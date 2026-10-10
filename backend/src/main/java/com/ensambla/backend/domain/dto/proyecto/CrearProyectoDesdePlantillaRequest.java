package com.ensambla.backend.domain.dto.proyecto;

import java.util.UUID;

public record CrearProyectoDesdePlantillaRequest(UUID usuarioId, UUID plantillaId) {
}

