package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.Optional;

public interface SuscripcionRepository {

    Suscripcion save(Suscripcion suscripcion);

    /** La suscripción es de la cuenta: vale para todos sus negocios. */
    Optional<Suscripcion> findByUsuarioId(UsuarioId usuarioId);

    default Suscripcion obtener(UsuarioId usuarioId) {
        return findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));
    }
}
