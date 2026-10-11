package com.ensambla.backend.domain.repository.bloque;

import com.ensambla.backend.domain.model.bloque.Bloque;

import java.util.Optional;
import java.util.UUID;

public interface BloqueRepository {

    Optional<Bloque> findByIdAndNegocioId(UUID id, UUID negocioId);

    Bloque save(Bloque bloque);

    /** Borra el bloque; la base elimina en cascada sus campos y registros. */
    void deleteByIdAndNegocioId(UUID id, UUID negocioId);
}
