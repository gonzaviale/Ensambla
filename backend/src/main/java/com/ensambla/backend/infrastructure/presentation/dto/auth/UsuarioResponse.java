package com.ensambla.backend.infrastructure.presentation.dto.auth;

import java.util.UUID;

public record UsuarioResponse(UUID id, String nombre, String email) {
}

