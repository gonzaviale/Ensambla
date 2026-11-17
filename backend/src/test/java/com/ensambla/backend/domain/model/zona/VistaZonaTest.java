package com.ensambla.backend.domain.model.zona;

import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VistaZonaTest {

    private static final List<String> PROPIOS = List.of("Yerba Playadito 1 kg");

    private AnalisisZona ordenada;

    @Test
    @DisplayName("Precios gasta el cupo de la zona")
    void preciosGastaElCupoDeLaZona() {
        entoncesElCupoDe(VistaZona.PRECIOS, TipoContenidoPlan.ZONA);
    }

    @Test
    @DisplayName("Qué se vende cerca gasta su propio cupo")
    void queSeVendeCercaGastaSuPropioCupo() {
        entoncesElCupoDe(VistaZona.CERCA, TipoContenidoPlan.ZONA_CERCA);
    }

    @Test
    @DisplayName("Otra zona gasta su propio cupo")
    void otraZonaGastaSuPropioCupo() {
        entoncesElCupoDe(VistaZona.OTRA, TipoContenidoPlan.ZONA_OTRA);
    }

    @Test
    @DisplayName("Precios ordena según el inventario del negocio")
    void preciosOrdenaSegunElInventarioDelNegocio() {
        entoncesUsaElInventario(VistaZona.PRECIOS, true);
    }

    @Test
    @DisplayName("Qué se vende cerca ordena según el inventario del negocio")
    void queSeVendeCercaOrdenaSegunElInventarioDelNegocio() {
        entoncesUsaElInventario(VistaZona.CERCA, true);
    }

    @Test
    @DisplayName("Otra zona no mira el inventario del negocio")
    void otraZonaNoMiraElInventarioDelNegocio() {
        entoncesUsaElInventario(VistaZona.OTRA, false);
    }

    @Test
    @DisplayName("Precios muestra primero lo que el negocio tiene")
    void preciosMuestraPrimeroLoQueElNegocioTiene() {
        cuandoSeOrdenaPara(VistaZona.PRECIOS);

        entoncesElOrdenEs("Yerba Playadito 500 g", "Azúcar", "Fideos");
    }

    @Test
    @DisplayName("Qué se vende cerca muestra primero lo más vendido que el negocio todavía no tiene")
    void queSeVendeCercaMuestraPrimeroLoMasVendidoQueElNegocioNoTiene() {
        cuandoSeOrdenaPara(VistaZona.CERCA);

        entoncesElOrdenEs("Fideos", "Azúcar", "Yerba Playadito 500 g");
    }

    @Test
    @DisplayName("Otra zona muestra lo más vendido sin mirar el inventario")
    void otraZonaMuestraLoMasVendidoSinMirarElInventario() {
        cuandoSeOrdenaPara(VistaZona.OTRA);

        entoncesElOrdenEs("Yerba Playadito 500 g", "Fideos", "Azúcar");
    }

    private void cuandoSeOrdenaPara(VistaZona vista) {
        ordenada = vista.ordenar(dadoAnalisis(), PROPIOS);
    }

    private void entoncesElCupoDe(VistaZona vista, TipoContenidoPlan esperado) {
        assertThat(vista.tipoDeContenido()).isEqualTo(esperado);
    }

    private void entoncesUsaElInventario(VistaZona vista, boolean esperado) {
        assertThat(vista.usaInventario()).isEqualTo(esperado);
    }

    private void entoncesElOrdenEs(String primero, String segundo, String tercero) {
        assertThat(ordenada.productos().stream().map(AgregadoZonaProducto::nombre).toList())
                .containsExactly(primero, segundo, tercero);
    }

    private static AnalisisZona dadoAnalisis() {
        return new AnalisisZona("San Justo", 10, List.of(
                dadoProducto("Azúcar", 4, "20"),
                dadoProducto("Yerba Playadito 500 g", 9, "5"),
                dadoProducto("Fideos", 6, "30")));
    }

    private static AgregadoZonaProducto dadoProducto(String nombre, int comerciosVenden, String unidadesSemana) {
        return new AgregadoZonaProducto(nombre, 8, comerciosVenden, new BigDecimal("100.00"),
                new BigDecimal("150.00"), new BigDecimal("200.00"), new BigDecimal(unidadesSemana));
    }
}
