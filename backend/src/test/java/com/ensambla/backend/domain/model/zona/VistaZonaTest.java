package com.ensambla.backend.domain.model.zona;

import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VistaZonaTest {

    private static final List<String> PROPIOS = List.of("Yerba Playadito 1 kg");

    @Test
    @DisplayName("Cada pantalla gasta su propio cupo gratuito")
    void tipoDeContenidoShouldBeDifferentForEachView() {
        assertThat(VistaZona.PRECIOS.tipoDeContenido()).isEqualTo(TipoContenidoPlan.ZONA);
        assertThat(VistaZona.CERCA.tipoDeContenido()).isEqualTo(TipoContenidoPlan.ZONA_CERCA);
        assertThat(VistaZona.OTRA.tipoDeContenido()).isEqualTo(TipoContenidoPlan.ZONA_OTRA);
    }

    @Test
    @DisplayName("Sólo Precios y Qué se vende cerca ordenan según el inventario del negocio")
    void usaInventarioShouldBeTrueOnlyForMyZoneViews() {
        assertThat(VistaZona.PRECIOS.usaInventario()).isTrue();
        assertThat(VistaZona.CERCA.usaInventario()).isTrue();
        assertThat(VistaZona.OTRA.usaInventario()).isFalse();
    }

    @Test
    @DisplayName("Precios muestra primero lo que el negocio tiene")
    void ordenarShouldPutOwnedFirstInPrices() {
        assertThat(nombres(VistaZona.PRECIOS.ordenar(givenAnalisis(), PROPIOS)))
                .containsExactly("Yerba Playadito 500 g", "Azúcar", "Fideos");
    }

    @Test
    @DisplayName("Qué se vende cerca muestra primero lo más vendido que el negocio todavía no tiene")
    void ordenarShouldPutBestSellersTheBusinessLacksFirstInNearby() {
        assertThat(nombres(VistaZona.CERCA.ordenar(givenAnalisis(), PROPIOS)))
                .containsExactly("Fideos", "Azúcar", "Yerba Playadito 500 g");
    }

    @Test
    @DisplayName("Otra zona muestra lo más vendido sin mirar el inventario")
    void ordenarShouldRankBySalesInOtherZone() {
        assertThat(nombres(VistaZona.OTRA.ordenar(givenAnalisis(), PROPIOS)))
                .containsExactly("Yerba Playadito 500 g", "Fideos", "Azúcar");
    }

    // --- helpers ---
    private static List<String> nombres(AnalisisZona analisis) {
        return analisis.productos().stream().map(AgregadoZonaProducto::nombre).toList();
    }

    private static AnalisisZona givenAnalisis() {
        return new AnalisisZona("San Justo", 10, List.of(
                givenProducto("Azúcar", 4, "20"),
                givenProducto("Yerba Playadito 500 g", 9, "5"),
                givenProducto("Fideos", 6, "30")));
    }

    private static AgregadoZonaProducto givenProducto(String nombre, int comerciosVenden, String unidadesSemana) {
        return new AgregadoZonaProducto(nombre, 8, comerciosVenden, new BigDecimal("100.00"),
                new BigDecimal("150.00"), new BigDecimal("200.00"), new BigDecimal(unidadesSemana));
    }
}
