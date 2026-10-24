package com.ensambla.backend.domain.repository.preferencia;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.Optional;

public interface PreferenciaRepository {

    Optional<PreferenciaUsuario> findByUsuarioId(UsuarioId usuarioId);

    PreferenciaUsuario save(PreferenciaUsuario preferencia);
}
