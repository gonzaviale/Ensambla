package com.ensambla.backend.domain.dto.zona;

import java.math.BigDecimal;

/**
 * Lo que se quiere analizar: un punto del mapa (latitud y longitud) o, si no hay punto, un lugar escrito
 * (barrio o dirección). Si vienen las dos cosas, manda el punto.
 */
public record AnalizarZonaRequest(String lugar, BigDecimal latitud, BigDecimal longitud) {

    public boolean tienePunto() {
        return latitud != null && longitud != null;
    }
}
