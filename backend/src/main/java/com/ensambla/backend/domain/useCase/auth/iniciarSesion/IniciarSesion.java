package com.ensambla.backend.domain.useCase.auth.iniciarSesion;

import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.service.usuario.UsuarioService;
import com.ensambla.backend.domain.useCase.auth.UsuarioResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IniciarSesion {

    private final UsuarioService usuarioService;

    public UsuarioResponse execute(IniciarSesionRequest request) {
        Usuario usuario = usuarioService.autenticar(request.email(), request.contrasenia());
        return UsuarioResponse.de(usuario);
    }
}

