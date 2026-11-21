package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.TipoPasoObjetivo;

import java.math.BigDecimal;

public record PasoObjetivoWebResponse(
        TipoPasoObjetivo tipo,
        String nombre,
        BigDecimal precio,
        BigDecimal costo,
        BigDecimal stock,
        BigDecimal vendidas30Dias,
        PrecioZonaWebResponse precioZona) {

    public record PrecioZonaWebResponse(BigDecimal minimo, BigDecimal promedio, BigDecimal maximo) {
    }
}
