package com.ensambla.backend.domain.useCase.auth.registrarUsuario;

import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.service.auth.RegistroService;
import com.ensambla.backend.domain.useCase.auth.UsuarioResponse;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class RegistrarUsuario {

    private final RegistroService registroService;
    private final Clock clock;

    public UsuarioResponse execute(RegistrarUsuarioRequest request) {
        Usuario usuario = registroService.registrar(request, OffsetDateTime.now(clock));
        return UsuarioResponse.de(usuario);
    }
}

