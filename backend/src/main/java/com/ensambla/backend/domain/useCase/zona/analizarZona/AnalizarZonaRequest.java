package com.ensambla.backend.domain.useCase.zona.analizarZona;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record AnalizarZonaRequest(String lugar, UsuarioId usuarioId) {
}
