package com.ensambla.backend.domain.useCase.auth;

import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record UsuarioResponse(UsuarioId id, String nombre, String email) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail());
    }
}
