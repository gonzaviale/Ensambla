package com.ensambla.backend.domain.model.objetivo;

import java.math.BigDecimal;

public record PasoObjetivo(
        TipoPasoObjetivo tipo,
        String nombre,
        BigDecimal precio,
        BigDecimal costo,
        BigDecimal stock,
        BigDecimal vendidas30Dias,
        PrecioZona precioZona) {

    public record PrecioZona(BigDecimal minimo, BigDecimal promedio, BigDecimal maximo) {
    }
}
