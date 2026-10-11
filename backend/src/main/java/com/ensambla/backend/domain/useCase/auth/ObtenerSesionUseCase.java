package com.ensambla.backend.domain.useCase.auth;

import com.ensambla.backend.domain.dto.auth.UsuarioResponse;
import com.ensambla.backend.domain.mapper.auth.ObtenerSesionMapper;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.service.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerSesionUseCase {

    private final UsuarioService usuarioService;
    private final ObtenerSesionMapper obtenerSesionMapper;

    public UsuarioResponse execute(UUID usuarioId) {
        Usuario usuario = usuarioService.obtenerActivo(usuarioId);
        return obtenerSesionMapper.toResponse(usuario);
    }
}

