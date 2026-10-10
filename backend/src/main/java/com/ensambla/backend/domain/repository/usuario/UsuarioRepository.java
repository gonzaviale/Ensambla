package com.ensambla.backend.domain.repository.usuario;

import com.ensambla.backend.domain.model.usuario.Usuario;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository {

    Usuario save(Usuario usuario);

    Optional<Usuario> findByEmail(String emailNormalizado);

    Optional<Usuario> findById(UUID id);
}
