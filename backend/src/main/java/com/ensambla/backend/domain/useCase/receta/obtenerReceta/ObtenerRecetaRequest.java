package com.ensambla.backend.domain.useCase.receta.obtenerReceta;

import java.util.UUID;

public record ObtenerRecetaRequest(UUID usuarioId, UUID registroId) {
}
