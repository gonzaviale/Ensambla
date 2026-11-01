package com.ensambla.backend.domain.repository.preferencia;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PreferenciaRepository {

    Optional<PreferenciaUsuario> findByUsuarioId(UsuarioId usuarioId);

    PreferenciaUsuario save(PreferenciaUsuario preferencia);

    /** Quien nunca guardó preferencias recibe las iniciales, sin que se escriba nada. */
    default PreferenciaUsuario obtenerOInicial(UsuarioId usuarioId, OffsetDateTime ahora) {
        return findByUsuarioId(usuarioId).orElseGet(() -> PreferenciaUsuario.inicial(usuarioId, ahora));
    }
}
