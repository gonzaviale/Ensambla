package com.ensambla.backend.domain.useCase.registro.obtenerRegistro;

import java.util.UUID;

public record ObtenerRegistroRequest(UUID usuarioId, UUID registroId) {
}
