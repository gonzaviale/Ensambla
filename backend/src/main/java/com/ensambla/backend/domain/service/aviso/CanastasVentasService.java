package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.model.aviso.ParProductos;
import com.ensambla.backend.domain.model.aviso.VentaProducto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Productos que se venden juntos. Como las ventas no tienen ticket, "la misma venta" (la canasta) es lo que un mismo
 * cliente se llevó el mismo día; las ventas sin cliente no forman canasta. No toca la base.
 */
@Service
public class CanastasVentasService {

    private record Canasta(String cliente, LocalDate fecha) {
    }

    private record Clave(UUID productoA, UUID productoB) {
    }

    /**
     * Los pares de productos distintos que estuvieron en la misma canasta al menos {@code minimoVeces} veces dentro del
     * período (hasta {@code diasPeriodo} días hacia atrás desde {@code hoy}). Cada par cuenta una vez por canasta, aunque
     * el producto se haya anotado varias veces. Primero los que más se repiten.
     */
    public List<ParProductos> paresFrecuentes(List<VentaProducto> ventas, LocalDate hoy, int diasPeriodo,
                                              int minimoVeces) {
        LocalDate desde = hoy.minusDays(diasPeriodo);
        Map<Clave, Integer> veces = new LinkedHashMap<>();
        canastas(ventas, desde, hoy).values().forEach(productos -> contarPares(productos, veces));
        return veces.entrySet().stream()
                .filter(par -> par.getValue() >= minimoVeces)
                .map(par -> ParProductos.de(par.getKey().productoA(), par.getKey().productoB(), par.getValue()))
                .sorted(Comparator.comparingInt(ParProductos::veces).reversed()
                        .thenComparing(par -> par.productoA().toString())
                        .thenComparing(par -> par.productoB().toString()))
                .toList();
    }

    private static Map<Canasta, Set<UUID>> canastas(List<VentaProducto> ventas, LocalDate desde, LocalDate hoy) {
        Map<Canasta, Set<UUID>> canastas = new LinkedHashMap<>();
        ventas.stream()
                .filter(venta -> venta.cliente() != null)
                .filter(venta -> venta.fecha().isAfter(desde) && !venta.fecha().isAfter(hoy))
                .forEach(venta -> canastas
                        .computeIfAbsent(new Canasta(venta.cliente(), venta.fecha()), canasta -> new LinkedHashSet<>())
                        .add(venta.productoId()));
        return canastas;
    }

    private static void contarPares(Set<UUID> productos, Map<Clave, Integer> veces) {
        List<UUID> ordenados = productos.stream().sorted(Comparator.comparing(UUID::toString)).toList();
        for (int i = 0; i < ordenados.size(); i++) {
            for (int j = i + 1; j < ordenados.size(); j++) {
                veces.merge(new Clave(ordenados.get(i), ordenados.get(j)), 1, Integer::sum);
            }
        }
    }
}
