package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.model.plan.Suscripcion;

import java.util.Optional;
import java.util.UUID;

public interface SuscripcionRepository {

    Suscripcion save(Suscripcion suscripcion);

    Optional<Suscripcion> findByNegocioId(UUID negocioId);
}
