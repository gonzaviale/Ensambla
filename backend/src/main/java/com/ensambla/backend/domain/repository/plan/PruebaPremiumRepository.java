package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.PruebaPremium;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PruebaPremiumRepository {

    Optional<PruebaPremium> findByNegocioId(NegocioId negocioId);

    PruebaPremium save(PruebaPremium prueba);

    List<NegocioId> findNegociosVencidos(OffsetDateTime ahora);
}
