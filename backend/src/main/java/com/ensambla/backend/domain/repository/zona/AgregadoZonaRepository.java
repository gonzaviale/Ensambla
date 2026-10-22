package com.ensambla.backend.domain.repository.zona;

import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;

import java.util.List;
import java.util.UUID;

public interface AgregadoZonaRepository {

    List<AgregadoZonaProducto> findByZonaId(UUID zonaId);

    /** Comercios de la zona que aceptaron aportar a los datos agregados. */
    long contarComerciosQueComparten(UUID zonaId);
}
