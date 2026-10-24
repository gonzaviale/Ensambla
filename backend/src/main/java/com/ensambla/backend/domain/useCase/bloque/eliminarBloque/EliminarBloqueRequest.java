package com.ensambla.backend.domain.useCase.bloque.eliminarBloque;

import java.util.UUID;

public record EliminarBloqueRequest(UUID usuarioId, UUID bloqueId) {
}
