package com.ensambla.backend.domain.repository.zona;

import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ZonaRepository {

    List<Zona> findAll();

    Optional<Zona> findById(ZonaId id);

    default Optional<Zona> resolver(BigDecimal latitud, BigDecimal longitud) {
        return Zona.masCercana(findAll(), latitud, longitud);
    }

    /** Un negocio sin zona asignada no tiene nada que buscar. */
    default Optional<Zona> findByIdSiTiene(ZonaId zonaId) {
        if (zonaId == null) {
            return Optional.empty();
        }
        return findById(zonaId);
    }
}
