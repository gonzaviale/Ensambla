package com.ensambla.backend.domain.model.zona;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalisisZonaTest {

    private static final String NOMBRE = "San Justo - UNLaM";
    private static final int MINIMO = 3;

    @Test
    @DisplayName("Sin zona no tiene nombre, comercios ni productos")
    void sinZonaShouldBeEmpty() {
        AnalisisZona analisis = AnalisisZona.sinZona();

        assertThat(analisis.nombre()).isEmpty();
        assertThat(analisis.comercios()).isZero();
        assertThat(analisis.productos()).isEmpty();
    }

    @Test
    @DisplayName("Quita los productos cuando hay menos comercios que el mínimo")
    void sinProductosSiHayMenosDeShouldDropProductsBelowMinimum() {
        AnalisisZona analisis = givenAnalisis(2, givenProducto("Yerba", 2, "5"));

        AnalisisZona result = analisis.sinProductosSiHayMenosDe(MINIMO);

        assertThat(result.nombre()).isEqualTo(NOMBRE);
        assertThat(result.comercios()).isEqualTo(2);
        assertThat(result.productos()).isEmpty();
    }

    @Test
    @DisplayName("Conserva los productos cuando los comercios alcanzan el mínimo")
    void sinProductosSiHayMenosDeShouldKeepProductsAtMinimum() {
        AnalisisZona analisis = givenAnalisis(MINIMO, givenProducto("Yerba", 2, "5"));

        AnalisisZona result = analisis.sinProductosSiHayMenosDe(MINIMO);

        assertThat(result).isSameAs(analisis);
    }

    @Test
    @DisplayName("Ordena por comercios que venden y desempata por unidades por semana")
    void ordenadoPorVentasShouldSortByCommercesThenUnits() {
        AnalisisZona analisis = givenAnalisis(10,
                givenProducto("Azúcar", 4, "20"),
                givenProducto("Yerba", 9, "5"),
                givenProducto("Fideos", 4, "30"));

        AnalisisZona result = analisis.ordenadoPorVentas();

        assertThat(result.productos()).extracting(AgregadoZonaProducto::nombre)
                .containsExactly("Yerba", "Fideos", "Azúcar");
        assertThat(result.nombre()).isEqualTo(NOMBRE);
        assertThat(result.comercios()).isEqualTo(10);
    }

    // --- helpers ---
    private AnalisisZona givenAnalisis(long comercios, AgregadoZonaProducto... productos) {
        return new AnalisisZona(NOMBRE, comercios, List.of(productos));
    }

    private AgregadoZonaProducto givenProducto(String nombre, int comerciosVenden, String unidadesSemana) {
        return new AgregadoZonaProducto(nombre, 8, comerciosVenden, new BigDecimal("100.00"),
                new BigDecimal("150.00"), new BigDecimal("200.00"), new BigDecimal(unidadesSemana));
    }
}
