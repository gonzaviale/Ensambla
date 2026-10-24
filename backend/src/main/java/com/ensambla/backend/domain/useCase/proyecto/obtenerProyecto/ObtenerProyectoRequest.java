package com.ensambla.backend.domain.useCase.proyecto.obtenerProyecto;

import java.util.UUID;

public record ObtenerProyectoRequest(UUID usuarioId, UUID proyectoId) {
}

