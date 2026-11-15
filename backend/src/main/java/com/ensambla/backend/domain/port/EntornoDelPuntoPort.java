package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface EntornoDelPuntoPort {

    /** Lo que hay a 500 m del punto (escuelas, oficinas, paradas, comercios…), medido en la fuente pública. */
    List<DatosZonaDondeAbrir.Punto> consultar(BigDecimal latitud, BigDecimal longitud, Instant instante);
}
