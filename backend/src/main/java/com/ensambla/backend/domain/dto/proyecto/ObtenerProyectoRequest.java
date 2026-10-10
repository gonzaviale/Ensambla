package com.ensambla.backend.domain.dto.proyecto;

import java.util.UUID;

public record ObtenerProyectoRequest(UUID usuarioId, UUID proyectoId) {
}

