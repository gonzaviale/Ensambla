package com.ensambla.backend.domain.dto.registro;

import java.util.UUID;

public record ObtenerFotoOrigenRequest(UUID usuarioId, UUID registroId) {
}
