package com.ensambla.backend.domain.repository.usuario;

import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.Optional;

public interface UsuarioRepository {

    Usuario save(Usuario usuario);

    Optional<Usuario> findByEmail(String emailNormalizado);

    Optional<Usuario> findById(UsuarioId id);

    /** El usuario de la sesión; uno inexistente o suspendido se trata como una sesión inválida. */
    default Usuario obtenerActivo(UsuarioId id) {
        return findById(id)
                .filter(Usuario::estaActivo)
                .orElseThrow(CredencialesInvalidasException::new);
    }
}
