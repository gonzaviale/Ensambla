package com.ensambla.backend.domain.dto.auth;

import java.util.UUID;

public record UsuarioResponse(UUID id, String nombre, String email) {
}

