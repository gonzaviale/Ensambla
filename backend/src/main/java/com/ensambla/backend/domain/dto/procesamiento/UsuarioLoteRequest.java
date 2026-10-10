package com.ensambla.backend.domain.dto.procesamiento;

import java.util.UUID;

public record UsuarioLoteRequest(UUID usuarioId, UUID loteId) {
}
