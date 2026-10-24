package com.ensambla.backend.domain.service.preferencia;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Lo comparten obtener y guardar preferencias: las del usuario o, si nunca guardó, las iniciales. */
@RequiredArgsConstructor
public class PreferenciaService {

    private final PreferenciaRepository preferenciaRepository;

    /** Quien nunca guardó preferencias recibe las iniciales, sin que se escriba nada. */
    public PreferenciaUsuario obtener(UUID usuarioId, OffsetDateTime ahora) {
        return preferenciaRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> PreferenciaUsuario.inicial(usuarioId, ahora));
    }
}
