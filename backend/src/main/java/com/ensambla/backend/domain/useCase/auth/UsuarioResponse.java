package com.ensambla.backend.domain.useCase.auth;

import com.ensambla.backend.domain.model.usuario.Usuario;

import java.util.UUID;

public record UsuarioResponse(UUID id, String nombre, String email) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail());
    }
}
