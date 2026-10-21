package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface GeocodificadorPort {

    List<DireccionEncontrada> buscar(String consulta, Instant instante);

    /** Geocodificación inversa: las localidades que el mapa reconoce en ese punto (vacío si no reconoce ninguna). */
    LocalidadesDelPunto resolverLocalidades(BigDecimal latitud, BigDecimal longitud, Instant instante);
}
