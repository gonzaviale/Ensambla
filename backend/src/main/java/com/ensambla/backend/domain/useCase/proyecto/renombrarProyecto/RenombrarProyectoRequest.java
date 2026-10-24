package com.ensambla.backend.domain.useCase.proyecto.renombrarProyecto;

import java.util.UUID;

public record RenombrarProyectoRequest(UUID usuarioId, UUID proyectoId, String nombre) {
}

