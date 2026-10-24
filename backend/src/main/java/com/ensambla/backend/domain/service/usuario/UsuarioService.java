package com.ensambla.backend.domain.service.usuario;

import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.port.ContraseniaPort;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ContraseniaPort contraseniaPort;

    public Usuario obtenerActivo(UUID id) {
        return usuarioRepository.findById(id)
                .filter(Usuario::estaActivo)
                .orElseThrow(CredencialesInvalidasException::new);
    }

    public Usuario autenticar(String email, String contrasenia) {
        return usuarioRepository.findByEmail(Usuario.normalizarEmail(email))
                .filter(Usuario::estaActivo)
                .filter(usuario -> contraseniaPort.coincide(contrasenia, usuario.getHashContrasenia()))
                .orElseThrow(CredencialesInvalidasException::new);
    }
}
