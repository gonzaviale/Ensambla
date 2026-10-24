package com.ensambla.backend.domain.useCase.registro.obtenerFotoOrigen;

import java.util.UUID;

public record ObtenerFotoOrigenRequest(UUID usuarioId, UUID registroId) {
}
