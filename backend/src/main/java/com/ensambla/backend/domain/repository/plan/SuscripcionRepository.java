package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.Suscripcion;

import java.util.Optional;

public interface SuscripcionRepository {

    Suscripcion save(Suscripcion suscripcion);

    Optional<Suscripcion> findByNegocioId(NegocioId negocioId);

    default Suscripcion obtener(NegocioId negocioId) {
        return findByNegocioId(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));
    }
}
