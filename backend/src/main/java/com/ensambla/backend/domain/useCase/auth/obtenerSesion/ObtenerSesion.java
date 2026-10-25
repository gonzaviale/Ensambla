package com.ensambla.backend.domain.useCase.auth.obtenerSesion;

import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.domain.useCase.auth.UsuarioResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerSesion {

    private final UsuarioRepository usuarioRepository;

    public UsuarioResponse execute(UsuarioId usuarioId) {
        Usuario usuario = usuarioRepository.obtenerActivo(usuarioId);
        return UsuarioResponse.de(usuario);
    }
}

