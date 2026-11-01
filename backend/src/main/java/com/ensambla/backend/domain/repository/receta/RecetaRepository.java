package com.ensambla.backend.domain.repository.receta;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.util.Optional;

public interface RecetaRepository {

    /** Guarda la receta; si el producto ya tenía una (mismo id), la reemplaza. */
    Receta save(Receta receta);

    Optional<Receta> findByProductoIdAndNegocioId(RegistroId productoId, NegocioId negocioId);

    void deleteByProductoIdAndNegocioId(RegistroId productoId, NegocioId negocioId);
}
