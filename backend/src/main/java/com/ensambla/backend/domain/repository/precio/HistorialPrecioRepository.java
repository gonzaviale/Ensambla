package com.ensambla.backend.domain.repository.precio;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.precio.CambioPrecio;

import java.time.OffsetDateTime;
import java.util.List;

public interface HistorialPrecioRepository {

    CambioPrecio save(CambioPrecio cambio);

    /** Los cambios de precio del negocio desde ese instante, del más viejo al más nuevo. */
    List<CambioPrecio> findByNegocioIdDesde(NegocioId negocioId, OffsetDateTime desde);
}
