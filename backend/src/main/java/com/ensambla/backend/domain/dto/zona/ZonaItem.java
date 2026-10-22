package com.ensambla.backend.domain.dto.zona;

import java.math.BigDecimal;
import java.util.List;

public record ZonaItem(String nombre, long comercios, List<ProductoItem> productos) {

    public record ProductoItem(
            String nombre,
            int comercios,
            BigDecimal precioMinimo,
            BigDecimal precioPromedio,
            BigDecimal precioMaximo,
            int comerciosVenden,
            BigDecimal unidadesPorSemana) {
    }
}
