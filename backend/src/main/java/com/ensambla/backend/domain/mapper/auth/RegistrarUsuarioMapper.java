package com.ensambla.backend.domain.mapper.auth;

import com.ensambla.backend.domain.dto.auth.UsuarioResponse;
import com.ensambla.backend.domain.model.usuario.Usuario;

public class RegistrarUsuarioMapper {

    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail());
    }
}

