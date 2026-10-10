package com.ensambla.backend.domain.useCase.auth;

import com.ensambla.backend.domain.dto.auth.IniciarSesionRequest;
import com.ensambla.backend.domain.dto.auth.UsuarioResponse;
import com.ensambla.backend.domain.mapper.auth.IniciarSesionMapper;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.service.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IniciarSesionUseCase {

    private final UsuarioService usuarioService;
    private final IniciarSesionMapper iniciarSesionMapper;

    public UsuarioResponse execute(IniciarSesionRequest request) {
        Usuario usuario = usuarioService.autenticar(request.email(), request.contrasenia());
        return iniciarSesionMapper.toResponse(usuario);
    }
}

