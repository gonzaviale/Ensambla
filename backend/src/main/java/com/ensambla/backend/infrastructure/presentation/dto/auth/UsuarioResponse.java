package com.ensambla.backend.infrastructure.presentation.dto.auth;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record UsuarioResponse(UsuarioId id, String nombre, String email) {
}

