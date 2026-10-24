package com.ensambla.backend.domain.useCase.zona.analizarZona;

import java.util.UUID;

public record AnalizarZonaRequest(String lugar, UUID usuarioId) {
}
