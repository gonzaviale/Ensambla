package com.ensambla.backend.domain.useCase.auth;

import com.ensambla.backend.domain.dto.auth.RegistrarUsuarioRequest;
import com.ensambla.backend.domain.dto.auth.UsuarioResponse;
import com.ensambla.backend.domain.mapper.auth.RegistrarUsuarioMapper;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.service.auth.RegistroService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class RegistrarUsuarioUseCase {

    private final RegistroService registroService;
    private final RegistrarUsuarioMapper registrarUsuarioMapper;
    private final Clock clock;

    public UsuarioResponse execute(RegistrarUsuarioRequest request) {
        Usuario usuario = registroService.registrar(request, OffsetDateTime.now(clock));
        return registrarUsuarioMapper.toResponse(usuario);
    }
}

