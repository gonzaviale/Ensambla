package com.ensambla.backend.domain.useCase.bloque.listarBloques;

import java.util.UUID;

public record ListarBloquesRequest(UUID usuarioId, UUID proyectoId) {
}
