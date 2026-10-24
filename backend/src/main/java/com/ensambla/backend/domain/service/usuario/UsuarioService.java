package com.ensambla.backend.domain.service.usuario;

import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public Usuario obtenerActivo(UsuarioId id) {
        return usuarioRepository.findById(id)
                .filter(Usuario::estaActivo)
                .orElseThrow(CredencialesInvalidasException::new);
    }
}
