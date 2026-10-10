package com.ensambla.backend.domain.dto.bloque;

import java.util.UUID;

public record RenombrarBloqueRequest(UUID usuarioId, UUID bloqueId, String nombre) {
}
