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

    @Test
    @DisplayName("Encuentra el producto de la zona sin importar tildes ni mayúsculas")
    void productoConRangoShouldFindProductIgnoringAccentsAndCase() {
        AnalisisZona analisis = givenAnalisis(10, givenProducto("Azúcar Ledesma 1 kg", 8, "5"));

        assertThat(analisis.productoConRango("AZUCAR ledesma 1 KG")).isPresent();
        assertThat(analisis.productoConRango("Yerba")).isEmpty();
    }

    @Test
    @DisplayName("No da rango si la zona tiene menos comercios que el mínimo")
    void productoConRangoShouldBeEmptyWhenZoneHasFewerStoresThanMinimum() {
        AnalisisZona analisis = givenAnalisis(2, givenProducto("Yerba", 8, "5"));

        assertThat(analisis.productoConRango("Yerba")).isEmpty();
    }

    @Test
    @DisplayName("No da rango si el producto lo cobran menos comercios que el mínimo")
    void productoConRangoShouldBeEmptyWhenProductHasFewerStoresThanMinimum() {
        AnalisisZona analisis = givenAnalisis(10, new AgregadoZonaProducto("Yerba", 2, 2, new BigDecimal("100"),
                new BigDecimal("150"), new BigDecimal("200"), new BigDecimal("5")));

        assertThat(analisis.productoConRango("Yerba")).isEmpty();
    }

    @Test
    @DisplayName("El ranking de más vendidos va por comercios que venden y desempata por unidades por semana")
    void masVendidosShouldRankByCommercesThenUnits() {
        AnalisisZona analisis = givenAnalisis(10,
                givenProducto("Azúcar", 4, "20"),
                givenProducto("Yerba", 9, "5"),
                givenProducto("Fideos", 4, "30"));

        assertThat(analisis.masVendidos()).extracting(AgregadoZonaProducto::nombre)
                .containsExactly("Yerba", "Fideos", "Azúcar");
    }

    @Test
    @DisplayName("El ranking deja afuera lo que venden menos comercios que el mínimo")
    void masVendidosShouldLeaveOutProductsSoldByFewerStoresThanMinimum() {
        AnalisisZona analisis = givenAnalisis(10, givenProducto("Yerba", 9, "5"), givenProducto("Sal", 2, "50"));

        assertThat(analisis.masVendidos()).extracting(AgregadoZonaProducto::nombre).containsExactly("Yerba");
    }

    @Test
    @DisplayName("No hay ranking cuando la zona tiene menos comercios que el mínimo")
    void masVendidosShouldBeEmptyWhenZoneHasFewerStoresThanMinimum() {
        AnalisisZona analisis = givenAnalisis(2, givenProducto("Yerba", 9, "5"));

        assertThat(analisis.masVendidos()).isEmpty();
    }

    @Test
    @DisplayName("Pone primero los productos que el negocio tiene y deja cada grupo en su orden")
    void conLosPropiosPrimeroShouldPutOwnedProductsFirstKeepingOrder() {
        AnalisisZona analisis = givenAnalisis(10,
                givenProducto("Azúcar Ledesma 1 kg", 9, "5"),
                givenProducto("Yerba Playadito 500 g", 8, "5"),
                givenProducto("Fideos Matarazzo 500 g", 7, "5"),
                givenProducto("Aceite Natura 900 ml", 6, "5"));

        AnalisisZona result = analisis.conLosPropiosPrimero(List.of("Aceite Natura 1 L", "Yerba Playadito 1 kg"));

        assertThat(result.productos()).extracting(AgregadoZonaProducto::nombre).containsExactly(
                "Yerba Playadito 500 g", "Aceite Natura 900 ml", "Azúcar Ledesma 1 kg", "Fideos Matarazzo 500 g");
        assertThat(result.nombre()).isEqualTo(NOMBRE);
        assertThat(result.comercios()).isEqualTo(10);
    }

    @Test
    @DisplayName("Sin productos propios deja los de la zona como estaban")
    void conLosPropiosPrimeroShouldKeepOrderWhenNegocioHasNoProducts() {
        AnalisisZona analisis = givenAnalisis(10, givenProducto("Yerba", 9, "5"), givenProducto("Azúcar", 8, "5"));

        AnalisisZona result = analisis.conLosPropiosPrimero(List.of());

        assertThat(result.productos()).extracting(AgregadoZonaProducto::nombre).containsExactly("Yerba", "Azúcar");
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
