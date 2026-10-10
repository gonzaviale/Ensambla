package com.ensambla.backend.domain.dto.bloque;

import java.util.UUID;

public record CrearBloqueRequest(UUID usuarioId, UUID proyectoId, String nombre) {
}
