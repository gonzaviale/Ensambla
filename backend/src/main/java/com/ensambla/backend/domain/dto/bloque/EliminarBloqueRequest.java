package com.ensambla.backend.domain.dto.bloque;

import java.util.UUID;

public record EliminarBloqueRequest(UUID usuarioId, UUID bloqueId) {
}
