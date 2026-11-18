package com.ensambla.backend.domain.model.zona;

import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.support.ZonasDePrueba.nombresDe;
import static com.ensambla.backend.support.ZonasDePrueba.unAnalisisDeZona;
import static com.ensambla.backend.support.ZonasDePrueba.unProductoDeZona;
import static org.assertj.core.api.Assertions.assertThat;

class VistaZonaTest {

    private static final List<String> PROPIOS = List.of("Yerba Playadito 1 kg");

    private final Map<VistaZona, TipoContenidoPlan> cupos = new EnumMap<>(VistaZona.class);
    private final Map<VistaZona, Boolean> usosDelInventario = new EnumMap<>(VistaZona.class);

    private AnalisisZona analisis;
    private AnalisisZona ordenado;

    @Test
    @DisplayName("Cada pantalla gasta su propio cupo gratuito")
    void cadaPantallaGastaSuPropioCupoGratuito() {
        cuandoSeConsultaElCupoDeCadaPantalla();

        entoncesCadaPantallaTieneSuCupo();
    }

    @Test
    @DisplayName("Sólo Precios y Qué se vende cerca ordenan según el inventario del negocio")
    void soloPreciosYQueSeVendeCercaOrdenanSegunElInventarioDelNegocio() {
        cuandoSePreguntaQuePantallasUsanElInventario();

        entoncesSoloUsanElInventarioPreciosYQueSeVendeCerca();
    }

    @Test
    @DisplayName("Precios muestra primero lo que el negocio tiene")
    void preciosMuestraPrimeroLoQueElNegocioTiene() {
        dadoUnaZonaConTresProductos();

        cuandoSeOrdenaParaLaPantalla(VistaZona.PRECIOS);

        entoncesLosProductosVanEnEsteOrden("Yerba Playadito 500 g", "Azúcar", "Fideos");
    }

    @Test
    @DisplayName("Qué se vende cerca muestra primero lo más vendido que el negocio todavía no tiene")
    void queSeVendeCercaMuestraPrimeroLoMasVendidoQueElNegocioTodaviaNoTiene() {
        dadoUnaZonaConTresProductos();

        cuandoSeOrdenaParaLaPantalla(VistaZona.CERCA);

        entoncesLosProductosVanEnEsteOrden("Fideos", "Azúcar", "Yerba Playadito 500 g");
    }

    @Test
    @DisplayName("Otra zona muestra lo más vendido sin mirar el inventario")
    void otraZonaMuestraLoMasVendidoSinMirarElInventario() {
        dadoUnaZonaConTresProductos();

        cuandoSeOrdenaParaLaPantalla(VistaZona.OTRA);

        entoncesLosProductosVanEnEsteOrden("Yerba Playadito 500 g", "Fideos", "Azúcar");
    }

    // --- dado ---
    private void dadoUnaZonaConTresProductos() {
        analisis = unAnalisisDeZona(10,
                unProductoDeZona("Azúcar", 4, "20"),
                unProductoDeZona("Yerba Playadito 500 g", 9, "5"),
                unProductoDeZona("Fideos", 6, "30"));
    }

    // --- cuando ---
    private void cuandoSeConsultaElCupoDeCadaPantalla() {
        for (VistaZona vista : VistaZona.values()) {
            cupos.put(vista, vista.tipoDeContenido());
        }
    }

    private void cuandoSePreguntaQuePantallasUsanElInventario() {
        for (VistaZona vista : VistaZona.values()) {
            usosDelInventario.put(vista, vista.usaInventario());
        }
    }

    private void cuandoSeOrdenaParaLaPantalla(VistaZona vista) {
        ordenado = vista.ordenar(analisis, PROPIOS);
    }

    // --- entonces ---
    private void entoncesCadaPantallaTieneSuCupo() {
        assertThat(cupos).containsEntry(VistaZona.PRECIOS, TipoContenidoPlan.ZONA)
                .containsEntry(VistaZona.CERCA, TipoContenidoPlan.ZONA_CERCA)
                .containsEntry(VistaZona.OTRA, TipoContenidoPlan.ZONA_OTRA);
    }

    private void entoncesSoloUsanElInventarioPreciosYQueSeVendeCerca() {
        assertThat(usosDelInventario).containsEntry(VistaZona.PRECIOS, true)
                .containsEntry(VistaZona.CERCA, true)
                .containsEntry(VistaZona.OTRA, false);
    }

    private void entoncesLosProductosVanEnEsteOrden(String... nombres) {
        assertThat(nombresDe(ordenado)).containsExactly(nombres);
    }
}
