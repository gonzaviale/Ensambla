package com.ensambla.backend.domain.repository.marca;

import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface KitDeMarcaRepository {

    Optional<KitDeMarca> findByNegocioId(NegocioId negocioId);

    KitDeMarca save(KitDeMarca kit);

    /** El kit del negocio, o el predeterminado (sin guardar) si todavía no armó el suyo. */
    default KitDeMarca obtenerOPredeterminado(NegocioId negocioId, OffsetDateTime ahora) {
        return findByNegocioId(negocioId).orElseGet(() -> KitDeMarca.predeterminado(negocioId, ahora));
    }
}
