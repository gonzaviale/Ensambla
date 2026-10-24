package com.ensambla.backend.domain.useCase.procesamiento;

import java.util.UUID;

public record UsuarioLoteRequest(UUID usuarioId, UUID loteId) {
}
