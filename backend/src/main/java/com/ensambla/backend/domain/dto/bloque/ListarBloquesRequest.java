package com.ensambla.backend.domain.dto.bloque;

import java.util.UUID;

public record ListarBloquesRequest(UUID usuarioId, UUID proyectoId) {
}
