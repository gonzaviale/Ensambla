package com.ensambla.backend.domain.dto.proyecto;

import java.util.UUID;

public record RenombrarProyectoRequest(UUID usuarioId, UUID proyectoId, String nombre) {
}

