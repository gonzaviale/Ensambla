package com.ensambla.backend.domain.repository.negocio;

import java.util.Optional;
import java.util.UUID;

public interface NegocioRepository {

    Optional<UUID> findIdByDuenioId(UUID usuarioId);
}

