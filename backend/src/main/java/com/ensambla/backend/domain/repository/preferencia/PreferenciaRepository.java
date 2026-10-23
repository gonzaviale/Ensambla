package com.ensambla.backend.domain.repository.preferencia;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;

import java.util.Optional;
import java.util.UUID;

public interface PreferenciaRepository {

    Optional<PreferenciaUsuario> findByUsuarioId(UUID usuarioId);

    PreferenciaUsuario save(PreferenciaUsuario preferencia);
}
