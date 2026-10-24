package com.ensambla.backend.domain.useCase.proyecto.eliminarProyecto;

import java.util.UUID;

public record EliminarProyectoRequest(UUID usuarioId, UUID proyectoId) {
}

