package com.ensambla.backend.infrastructure.presentation.dto.zona;

import java.math.BigDecimal;
import java.util.List;

public record ZonaWebResponse(String nombre, long comercios, List<ProductoWebResponse> productos) {

    public record ProductoWebResponse(
            String nombre, int comercios, PrecioWebResponse precio, VentasWebResponse ventas) {
    }

    public record PrecioWebResponse(BigDecimal minimo, BigDecimal promedio, BigDecimal maximo) {
    }

    public record VentasWebResponse(int comercios, BigDecimal unidadesPorSemana) {
    }
}
