package com.ensambla.backend.domain.repository.usuario;

import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.Optional;

public interface UsuarioRepository {

    Usuario save(Usuario usuario);

    Optional<Usuario> findByEmail(String emailNormalizado);

    Optional<Usuario> findById(UsuarioId id);
}
