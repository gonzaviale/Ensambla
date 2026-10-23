package com.ensambla.backend.domain.model.zona;

import java.util.List;

/** Lo que se sabe de una zona: cuántos comercios aportan y los precios y ventas agregados de sus productos. */
public record AnalisisZona(String nombre, long comercios, List<AgregadoZonaProducto> productos) {

    /** Para quien todavía no tiene zona: el front lo muestra como "sin datos" en vez de fallar. */
    public static AnalisisZona sinZona() {
        return new AnalisisZona("", 0, List.of());
    }
}
