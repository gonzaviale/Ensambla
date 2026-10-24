package com.ensambla.backend.domain.useCase.auth.obtenerSesion;

import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.service.usuario.UsuarioService;
import com.ensambla.backend.domain.useCase.auth.UsuarioResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerSesion {

    private final UsuarioService usuarioService;

    public UsuarioResponse execute(UsuarioId usuarioId) {
        Usuario usuario = usuarioService.obtenerActivo(usuarioId);
        return UsuarioResponse.de(usuario);
    }
}

