package com.ensambla.backend.domain.dto.zona;

import java.util.UUID;

public record AnalizarZonaRequest(String lugar, UUID usuarioId) {
}
