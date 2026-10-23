package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.model.aviso.ParProductos;
import com.ensambla.backend.domain.model.aviso.VentaProducto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CanastasVentasServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final int PERIODO = 30;
    private static final int MINIMO = 3;
    private static final UUID P1 = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID P2 = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID P3 = UUID.fromString("d0000000-0000-4000-8000-000000000003");
    private static final String ROSA = "Doña Rosa";
    private static final String BAR = "Bar La Esquina";

    private final CanastasVentasService servicio = new CanastasVentasService();

    @Test
    @DisplayName("Cuenta una vez cada canasta (mismo cliente y mismo día) que tiene a los dos productos")
    void paresFrecuentesShouldCountEachBasketOnce() {
        List<VentaProducto> ventas = new ArrayList<>();
        ventas.addAll(canasta(ROSA, "2026-10-01", P1, P2));
        ventas.addAll(canasta(ROSA, "2026-10-08", P2, P1));
        ventas.addAll(canasta(BAR, "2026-10-15", P1, P2));

        List<ParProductos> pares = whenPares(ventas);

        assertThat(pares).containsExactly(new ParProductos(P1, P2, 3));
    }

    @Test
    @DisplayName("Un producto anotado varias veces en la misma canasta no repite el par")
    void paresFrecuentesShouldCountRepeatedProductOnceInTheSameBasket() {
        List<VentaProducto> ventas = new ArrayList<>();
        for (String dia : List.of("2026-10-01", "2026-10-08", "2026-10-15")) {
            ventas.addAll(canasta(ROSA, dia, P1, P1, P2));
        }

        assertThat(whenPares(ventas)).containsExactly(new ParProductos(P1, P2, 3));
    }

    @Test
    @DisplayName("Dos clientes distintos el mismo día no forman una canasta")
    void paresFrecuentesShouldSeparateBasketsOfDifferentCustomers() {
        List<VentaProducto> ventas = new ArrayList<>();
        for (String dia : List.of("2026-10-01", "2026-10-08", "2026-10-15")) {
            ventas.add(venta(P1, dia, ROSA));
            ventas.add(venta(P2, dia, BAR));
        }

        assertThat(whenPares(ventas)).isEmpty();
    }

    @Test
    @DisplayName("Las ventas sin cliente no forman canasta")
    void paresFrecuentesShouldIgnoreSalesWithoutCustomer() {
        List<VentaProducto> ventas = new ArrayList<>();
        for (String dia : List.of("2026-10-01", "2026-10-08", "2026-10-15")) {
            ventas.addAll(canasta(null, dia, P1, P2));
        }

        assertThat(whenPares(ventas)).isEmpty();
    }

    @Test
    @DisplayName("Ignora las canastas anteriores al período y las del futuro")
    void paresFrecuentesShouldIgnoreBasketsOutsideThePeriod() {
        List<VentaProducto> ventas = new ArrayList<>();
        ventas.addAll(canasta(ROSA, "2026-10-01", P1, P2));
        ventas.addAll(canasta(ROSA, "2026-10-08", P1, P2));
        // el día que abre el período (hoy - 30) ya no cuenta, y mañana todavía no
        ventas.addAll(canasta(ROSA, "2026-09-19", P1, P2));
        ventas.addAll(canasta(ROSA, "2026-10-20", P1, P2));

        assertThat(whenPares(ventas)).isEmpty();
    }

    @Test
    @DisplayName("Sólo devuelve los pares que llegan al mínimo de veces")
    void paresFrecuentesShouldReturnOnlyPairsReachingTheMinimum() {
        List<VentaProducto> ventas = new ArrayList<>();
        ventas.addAll(canasta(ROSA, "2026-10-01", P1, P2));
        ventas.addAll(canasta(ROSA, "2026-10-08", P1, P2));

        assertThat(whenPares(ventas)).isEmpty();
        assertThat(servicio.paresFrecuentes(ventas, HOY, PERIODO, 2)).containsExactly(new ParProductos(P1, P2, 2));
    }

    @Test
    @DisplayName("Una canasta de tres productos suma los tres pares y salen primero los que más se repiten")
    void paresFrecuentesShouldCombineAllProductsOfABasketAndSortByTimes() {
        List<VentaProducto> ventas = new ArrayList<>();
        for (String dia : List.of("2026-10-01", "2026-10-08", "2026-10-15")) {
            ventas.addAll(canasta(ROSA, dia, P1, P2, P3));
        }
        ventas.addAll(canasta(BAR, "2026-10-16", P2, P3));

        assertThat(whenPares(ventas)).containsExactly(
                new ParProductos(P2, P3, 4),
                new ParProductos(P1, P2, 3),
                new ParProductos(P1, P3, 3));
    }

    // --- act ---
    private List<ParProductos> whenPares(List<VentaProducto> ventas) {
        return servicio.paresFrecuentes(ventas, HOY, PERIODO, MINIMO);
    }

    // --- helpers ---
    private static List<VentaProducto> canasta(String cliente, String fecha, UUID... productos) {
        return Arrays.stream(productos).map(producto -> venta(producto, fecha, cliente)).toList();
    }

    private static VentaProducto venta(UUID producto, String fecha, String cliente) {
        return new VentaProducto(producto, BigDecimal.ONE, null, LocalDate.parse(fecha), cliente);
    }
}
