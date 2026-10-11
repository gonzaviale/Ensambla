package com.ensambla.backend.domain.dto.registro;

import java.util.UUID;

public record ObtenerRegistroRequest(UUID usuarioId, UUID registroId) {
}
