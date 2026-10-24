package com.ensambla.backend.domain.useCase.bloque.crearBloque;

import java.util.UUID;

public record CrearBloqueRequest(UUID usuarioId, UUID proyectoId, String nombre) {
}
