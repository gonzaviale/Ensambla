package com.ensambla.backend.domain.repository.negocio;

import com.ensambla.backend.domain.model.negocio.Negocio;

import java.util.Optional;
import java.util.UUID;

public interface NegocioRepository {

    Optional<UUID> findIdByDuenioId(UUID usuarioId);

    Negocio save(Negocio negocio);
}

