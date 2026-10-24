package com.ensambla.backend.domain.useCase.auth.iniciarSesion;

import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.port.ContraseniaPort;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.domain.useCase.auth.UsuarioResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IniciarSesion {

    private final UsuarioRepository usuarioRepository;

    private final ContraseniaPort contraseniaPort;

    public UsuarioResponse execute(IniciarSesionRequest request) {
        return UsuarioResponse.de(autenticar(request.email(), request.contrasenia()));
    }

    /** Un email desconocido, una cuenta suspendida o una contraseña incorrecta responden igual. */
    private Usuario autenticar(String email, String contrasenia) {
        return usuarioRepository.findByEmail(Usuario.normalizarEmail(email))
                .filter(Usuario::estaActivo)
                .filter(usuario -> contraseniaPort.coincide(contrasenia, usuario.getHashContrasenia()))
                .orElseThrow(CredencialesInvalidasException::new);
    }
}
