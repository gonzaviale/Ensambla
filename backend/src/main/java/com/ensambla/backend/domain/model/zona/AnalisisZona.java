package com.ensambla.backend.domain.model.zona;

import com.ensambla.backend.domain.model.common.TextoNormalizado;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

    /**
     * Los agregados de un producto (mismo nombre, sin importar tildes ni mayúsculas), sólo si la zona y el producto
     * tienen al menos {@link #MINIMO_COMERCIOS} comercios: con menos, el rango dejaría adivinar qué cobra cada uno.
     */
    public Optional<AgregadoZonaProducto> productoConRango(String nombre) {
        if (comercios < MINIMO_COMERCIOS) {
            return Optional.empty();
        }
        return productos.stream()
                .filter(producto -> producto.comercios() >= MINIMO_COMERCIOS)
                .filter(producto -> TextoNormalizado.de(producto.nombre()).equals(TextoNormalizado.de(nombre)))
                .findFirst();
    }

    /**
     * El ranking de lo que más se vende en la zona: sólo los productos que venden {@link #MINIMO_COMERCIOS} comercios o
     * más, primero los que más comercios venden y, a igualdad, los que más unidades por semana mueven. Con menos
     * comercios en la zona no hay ranking: dejaría adivinar qué vende cada uno.
     */
    public List<AgregadoZonaProducto> masVendidos() {
        if (comercios < MINIMO_COMERCIOS) {
            return List.of();
        }
        List<AgregadoZonaProducto> anonimos = productos.stream()
                .filter(producto -> producto.comerciosVenden() >= MINIMO_COMERCIOS)
                .toList();
        return new AnalisisZona(nombre, comercios, anonimos).ordenadoPorVentas().productos();
    }

    /**
     * Primero los productos que el negocio ya tiene ({@link NombreDeProducto#esElMismo}) y después el resto, cada grupo en
     * su orden. Sirve para que, con un cupo de productos que se pueden ver, entren antes los que sirven para comparar.
     */
    public AnalisisZona conLosPropiosPrimero(List<String> nombresPropios) {
        return separadosPor(nombresPropios, true);
    }

    /** Al revés: primero lo que el negocio todavía no tiene y al final lo que ya tiene, cada grupo en su orden. */
    public AnalisisZona conLosQueFaltanPrimero(List<String> nombresPropios) {
        return separadosPor(nombresPropios, false);
    }

    private AnalisisZona separadosPor(List<String> nombresPropios, boolean propiosPrimero) {
        Predicate<AgregadoZonaProducto> esPropio = producto -> nombresPropios.stream()
                .anyMatch(nombre -> NombreDeProducto.esElMismo(nombre, producto.nombre()));
        Predicate<AgregadoZonaProducto> primero = propiosPrimero ? esPropio : esPropio.negate();
        List<AgregadoZonaProducto> ordenados = Stream.concat(
                productos.stream().filter(primero),
                productos.stream().filter(primero.negate())).toList();
        return new AnalisisZona(nombre, comercios, ordenados);
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
