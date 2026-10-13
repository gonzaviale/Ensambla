package com.ensambla.backend.domain.dto.receta;

import java.util.UUID;

public record ObtenerRecetaRequest(UUID usuarioId, UUID registroId) {
}
