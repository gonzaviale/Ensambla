package com.ensambla.backend.domain.model.zona;

import java.util.Comparator;
import java.util.List;

/** Lo que se sabe de una zona: cuántos comercios aportan y los precios y ventas agregados de sus productos. */
public record AnalisisZona(String nombre, long comercios, List<AgregadoZonaProducto> productos) {

    /** Menos comercios que esto y los agregados dejarían adivinar qué vende o cobra cada uno. */
    public static final int MINIMO_COMERCIOS = 3;

    /** Para quien todavía no tiene zona: el front lo muestra como "sin datos" en vez de fallar. */
    public static AnalisisZona sinZona() {
        return new AnalisisZona("", 0, List.of());
    }

    /**
     * Con pocos comercios los agregados no son anónimos: por debajo del mínimo se conserva el nombre y la
     * cantidad, pero no los productos.
     */
    public AnalisisZona sinProductosSiHayMenosDe(int minimoComercios) {
        if (comercios >= minimoComercios) {
            return this;
        }
        return new AnalisisZona(nombre, comercios, List.of());
    }

    /** Primero los que más comercios venden y, a igualdad, los que más unidades por semana mueven. */
    public AnalisisZona ordenadoPorVentas() {
        List<AgregadoZonaProducto> ordenados = productos.stream()
                .sorted(Comparator.comparingInt(AgregadoZonaProducto::comerciosVenden).reversed()
                        .thenComparing(AgregadoZonaProducto::unidadesSemana, Comparator.reverseOrder()))
                .toList();
        return new AnalisisZona(nombre, comercios, ordenados);
    }
}
