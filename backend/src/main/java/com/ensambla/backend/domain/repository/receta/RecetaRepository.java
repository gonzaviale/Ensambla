package com.ensambla.backend.domain.repository.receta;

import com.ensambla.backend.domain.model.receta.Receta;

import java.util.Optional;
import java.util.UUID;

public interface RecetaRepository {

    /** Guarda la receta; si el producto ya tenía una (mismo id), la reemplaza. */
    Receta save(Receta receta);

    Optional<Receta> findByProductoIdAndNegocioId(UUID productoId, UUID negocioId);

    void deleteByProductoIdAndNegocioId(UUID productoId, UUID negocioId);
}
