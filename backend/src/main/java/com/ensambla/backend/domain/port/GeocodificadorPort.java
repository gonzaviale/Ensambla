package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;

import java.time.Instant;
import java.util.List;

public interface GeocodificadorPort {

    List<DireccionEncontrada> buscar(String consulta, Instant instante);
}
