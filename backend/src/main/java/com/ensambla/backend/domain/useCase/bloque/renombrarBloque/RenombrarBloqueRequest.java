package com.ensambla.backend.domain.useCase.bloque.renombrarBloque;

import java.util.UUID;

public record RenombrarBloqueRequest(UUID usuarioId, UUID bloqueId, String nombre) {
}
