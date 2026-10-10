package com.ensambla.backend.domain.repository.bloque;

import com.ensambla.backend.domain.model.bloque.Bloque;

import java.util.Optional;
import java.util.UUID;

public interface BloqueRepository {

    Optional<Bloque> findByIdAndNegocioId(UUID id, UUID negocioId);
}
