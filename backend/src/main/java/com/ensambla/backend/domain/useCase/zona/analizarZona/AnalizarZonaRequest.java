package com.ensambla.backend.domain.useCase.zona.analizarZona;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.math.BigDecimal;

/**
 * Lo que se quiere analizar: un punto del mapa (latitud y longitud) o, si no hay punto, un lugar escrito
 * (barrio o dirección). Si vienen las dos cosas, manda el punto.
 */
public record AnalizarZonaRequest(String lugar, BigDecimal latitud, BigDecimal longitud, UsuarioId usuarioId) {

    public boolean tienePunto() {
        return latitud != null && longitud != null;
    }
}
