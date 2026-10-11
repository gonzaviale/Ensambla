package com.ensambla.backend.domain.dto.registro;

import java.util.UUID;

public record EliminarRegistroRequest(UUID usuarioId, UUID registroId) {
}
