package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.model.plan.PruebaPremium;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PruebaPremiumRepository {

    Optional<PruebaPremium> findByNegocioId(UUID negocioId);

    PruebaPremium save(PruebaPremium prueba);

    List<UUID> findNegociosVencidos(OffsetDateTime ahora);
}
