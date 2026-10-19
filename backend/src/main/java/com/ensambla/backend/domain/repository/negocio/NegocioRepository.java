package com.ensambla.backend.domain.repository.negocio;

import com.ensambla.backend.domain.model.negocio.Negocio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NegocioRepository {

    Optional<UUID> findIdByDuenioId(UUID usuarioId);

    Negocio save(Negocio negocio);

    /** Ids de todos los negocios; los usa el motor de monitoreo para recorrerlos. */
    List<UUID> findAllIds();
}

