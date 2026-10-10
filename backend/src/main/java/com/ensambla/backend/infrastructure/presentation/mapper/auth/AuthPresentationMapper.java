package com.ensambla.backend.infrastructure.presentation.mapper.auth;

import com.ensambla.backend.domain.dto.auth.IniciarSesionRequest;
import com.ensambla.backend.domain.dto.auth.RegistrarUsuarioRequest;
import com.ensambla.backend.infrastructure.presentation.dto.auth.LoginRequest;
import com.ensambla.backend.infrastructure.presentation.dto.auth.RegistroRequest;
import com.ensambla.backend.infrastructure.presentation.dto.auth.UsuarioResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthPresentationMapper {

    public RegistrarUsuarioRequest toRequest(RegistroRequest request) {
        return new RegistrarUsuarioRequest(request.nombre(), request.nombreNegocio(),
                request.email().trim(), request.contrasenia());
    }

    public IniciarSesionRequest toRequest(LoginRequest request) {
        return new IniciarSesionRequest(request.email().trim(), request.contrasenia());
    }

    public UsuarioResponse toResponse(com.ensambla.backend.domain.dto.auth.UsuarioResponse response) {
        return new UsuarioResponse(response.id(), response.nombre(), response.email());
    }
}

