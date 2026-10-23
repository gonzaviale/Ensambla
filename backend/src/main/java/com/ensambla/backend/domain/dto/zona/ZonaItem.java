package com.ensambla.backend.domain.dto.zona;

import com.ensambla.backend.domain.dto.plan.AccesoContenidoItem;

import java.math.BigDecimal;
import java.util.List;

public record ZonaItem(String nombre, long comercios, List<ProductoItem> productos, AccesoContenidoItem acceso) {

    public ZonaItem(String nombre, long comercios, List<ProductoItem> productos) {
        this(nombre, comercios, productos, null);
    }

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
