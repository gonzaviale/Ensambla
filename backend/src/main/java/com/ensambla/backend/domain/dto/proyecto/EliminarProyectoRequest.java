package com.ensambla.backend.domain.dto.proyecto;

import java.util.UUID;

public record EliminarProyectoRequest(UUID usuarioId, UUID proyectoId) {
}

