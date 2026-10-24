package com.ensambla.backend.domain.repository.bloque;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.util.Optional;

public interface BloqueRepository {

    Optional<Bloque> findByIdAndNegocioId(BloqueId id, NegocioId negocioId);

    Bloque save(Bloque bloque);

    /** Borra el bloque; la base elimina en cascada sus campos y registros. */
    void deleteByIdAndNegocioId(BloqueId id, NegocioId negocioId);
}
