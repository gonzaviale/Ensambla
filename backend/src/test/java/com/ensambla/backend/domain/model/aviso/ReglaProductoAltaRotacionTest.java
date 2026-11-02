package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReglaProductoAltaRotacionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final UUID YERBA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final String NOMBRE = "Yerba Nobleza Gaucha 1 kg";

    private final ReglaProductoAltaRotacion regla = new ReglaProductoAltaRotacion(
            new MetricasVentas(30, new BigDecimal("0.5")));

    @Test
    @DisplayName("Es la regla de los avisos de producto muy vendido")
    void tipoShouldBeMuyVendido() {
        assertThat(regla.tipo()).isEqualTo(TipoAviso.MUY_VENDIDO);
    }

    @Test
    @DisplayName("Avisa del producto que vende más que su promedio y está bajo el rango de la zona, con el aumento sugerido")
    void evaluarShouldSuggestRaisePriceWhenSellingAboveAverageAndBelowZoneRange() {
        List<ResultadoRegla> resultados = whenEvaluar(zona(3500, 4800, 8), ventasEnAlza(), yerba(3400, 2700));

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.entidad()).isEqualTo("registro:" + YERBA_ID);
            assertThat(resultado.prioridad()).isEqualTo((short) 1);
            assertThat(resultado.proyectoId()).isEqualTo(AvisoTestData.PROYECTO_ID);
            assertThat(resultado.titulo())
                    .isEqualTo("Yerba Nobleza Gaucha 1 kg se vende más que de costumbre y lo tenés por debajo de tu zona");
            assertThat(resultado.detalle()).isEqualTo("Vendiste 20 en los últimos 30 días, más que tu promedio de 14,75 "
                    + "por período, y lo tenés a $3.400 cuando en tu zona se vende entre $3.500 y $4.800. "
                    + "Si lo subís a $3.500, queda dentro del rango.");
            assertThat(resultado.motivos()).containsExactly(
                    "Vendiste 20 unidades en los últimos 30 días y tu promedio por período es 14,75.",
                    "En tu zona, 8 comercios lo venden entre $3.500 y $4.800.",
                    "Hoy te quedan $700 por unidad; a $3.500 te quedarían $800.");
            assertThat(resultado.datos()).containsEntry("precio", new BigDecimal("3400"))
                    .containsEntry("sugerido", new BigDecimal("3500"));
        });
    }

    @Test
    @DisplayName("Ofrece subir el precio con una acción que edita el registro y otra que abre el producto")
    void evaluarShouldOfferSubirAndAbrirActions() {
        List<ResultadoRegla> resultados = whenEvaluar(zona(3500, 4800, 8), ventasEnAlza(), yerba(3400, 2700));

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.acciones()).hasSize(2);
            assertThat(resultado.acciones().get(0)).isEqualTo(Map.of(
                    "tipo", "EDITAR_REGISTRO",
                    "texto", "Subir a $3.500",
                    "proyectoId", AvisoTestData.PROYECTO_ID.toString(),
                    "bloqueId", AvisoTestData.INVENTARIO_ID.toString(),
                    "registroId", YERBA_ID.toString(),
                    "valores", Map.of("price", new BigDecimal("3500"))));
            assertThat(resultado.acciones().get(1)).isEqualTo(Map.of(
                    "tipo", "ABRIR",
                    "texto", "Ver producto",
                    "proyectoId", AvisoTestData.PROYECTO_ID.toString(),
                    "bloqueId", AvisoTestData.INVENTARIO_ID.toString(),
                    "registroId", YERBA_ID.toString()));
        });
    }

    @Test
    @DisplayName("Sin costo cargado no habla del margen")
    void evaluarShouldOmitMarginWhenProductHasNoCost() {
        List<ResultadoRegla> resultados = whenEvaluar(zona(3500, 4800, 8), ventasEnAlza(), yerba(3400, null));

        assertThat(resultados).singleElement().satisfies(resultado -> assertThat(resultado.motivos()).hasSize(2));
        assertThat(whenEvaluar(zona(3500, 4800, 8), ventasEnAlza(), yerba(3400, 0)).get(0).motivos()).hasSize(2);
    }

    @Test
    @DisplayName("Si redondear el mínimo a la decena no lo deja por encima del precio actual, sugiere el mínimo exacto")
    void evaluarShouldSuggestExactMinimumWhenRoundingDoesNotRaiseThePrice() {
        List<ResultadoRegla> resultados = whenEvaluar(zona(3494, 4800, 8), ventasEnAlza(), yerba(3492, null));

        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.datos()).containsEntry("sugerido", new BigDecimal("3494")));
    }

    @Test
    @DisplayName("No avisa si las ventas del período no superan su promedio")
    void evaluarShouldReturnNothingWhenSalesDoNotExceedAverage() {
        List<Registro> soloUnaVenta = List.of(venta(20, "2026-10-10"));

        assertThat(whenEvaluar(zona(3500, 4800, 8), soloUnaVenta, yerba(3400, 2700))).isEmpty();
    }

    @Test
    @DisplayName("No avisa si el precio ya está dentro del rango de la zona, incluido el mínimo")
    void evaluarShouldReturnNothingWhenPriceIsNotBelowZoneRange() {
        assertThat(whenEvaluar(zona(3500, 4800, 8), ventasEnAlza(), yerba(3500, 2700))).isEmpty();
        assertThat(whenEvaluar(zona(3500, 4800, 8), ventasEnAlza(), yerba(4000, 2700))).isEmpty();
    }

    @Test
    @DisplayName("No avisa si el producto no está en la zona o la zona no alcanza el mínimo de comercios")
    void evaluarShouldReturnNothingWithoutAnonymousZoneRange() {
        assertThat(whenEvaluar(zonaConProducto("Fideos Luchetti 500 g", 8), ventasEnAlza(), yerba(3400, 2700))).isEmpty();
        assertThat(whenEvaluar(zonaConProducto(NOMBRE, 2), ventasEnAlza(), yerba(3400, 2700))).isEmpty();
        assertThat(whenEvaluar(AnalisisZona.sinZona(), ventasEnAlza(), yerba(3400, 2700))).isEmpty();
    }

    @Test
    @DisplayName("No avisa de un producto sin ventas anotadas ni de uno sin precio")
    void evaluarShouldReturnNothingWithoutSalesOrPrice() {
        assertThat(whenEvaluar(zona(3500, 4800, 8), List.of(), yerba(3400, 2700))).isEmpty();
        assertThat(whenEvaluar(zona(3500, 4800, 8), ventasEnAlza(), yerba(null, 2700))).isEmpty();
        assertThat(whenEvaluar(zona(3500, 4800, 8), ventasEnAlza(), yerba(0, 2700))).isEmpty();
    }

    @Test
    @DisplayName("No avisa nada sin la fecha de hoy")
    void evaluarShouldReturnNothingWhenHoyIsMissing() {
        ContextoNegocio contexto = new ContextoNegocio(AvisoTestData.NEGOCIO_ID,
                List.of(AvisoTestData.inventarioConPrecios(yerba(3400, 2700))));

        assertThat(regla.evaluar(contexto)).isEmpty();
    }

    @Test
    @DisplayName("La huella no cambia con las ventas pero sí cuando cambia el precio")
    void evaluarShouldKeepHuellaWhenOnlySalesChangeAndChangeItWhenPriceChanges() {
        ResultadoRegla base = whenEvaluar(zona(3500, 4800, 8), ventasEnAlza(), yerba(3400, 2700)).get(0);
        List<Registro> masVentas = List.of(venta(10, "2026-08-20"), venta(25, "2026-10-10"));
        ResultadoRegla conOtrasVentas = whenEvaluar(zona(3500, 4800, 8), masVentas, yerba(3400, 2700)).get(0);
        ResultadoRegla otroPrecio = whenEvaluar(zona(3500, 4800, 8), ventasEnAlza(), yerba(3450, 2700)).get(0);

        assertThat(conOtrasVentas.huellaPara(TipoAviso.MUY_VENDIDO)).isEqualTo(base.huellaPara(TipoAviso.MUY_VENDIDO));
        assertThat(otroPrecio.huellaPara(TipoAviso.MUY_VENDIDO)).isNotEqualTo(base.huellaPara(TipoAviso.MUY_VENDIDO));
    }

    // --- act ---
    private List<ResultadoRegla> whenEvaluar(AnalisisZona zona, List<Registro> ventas, Registro... productos) {
        BloqueInventario inventario = AvisoTestData.inventarioConPrecios(productos);
        return regla.evaluar(new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY, List.of(inventario),
                List.of(AvisoTestData.ventas(ventas.toArray(new Registro[0]))), zona));
    }

    // --- helpers ---
    private static Registro yerba(Object precio, Object costo) {
        return AvisoTestData.producto(YERBA_ID, NOMBRE, 40, precio, costo);
    }

    /** 10 unidades hace dos meses y 20 en el último período: 20 supera el promedio de 14,75 por período. */
    private static List<Registro> ventasEnAlza() {
        return List.of(venta(10, "2026-08-20"), venta(20, "2026-10-10"));
    }

    private static Registro venta(int cantidad, String fecha) {
        return AvisoTestData.venta(YERBA_ID.toString(), cantidad, 3400, fecha, null);
    }

    private static AnalisisZona zona(int minimo, int maximo, int comercios) {
        return new AnalisisZona("San Justo", 10, List.of(producto(NOMBRE, minimo, maximo, comercios)));
    }

    private static AnalisisZona zonaConProducto(String nombre, int comercios) {
        return new AnalisisZona("San Justo", 10, List.of(producto(nombre, 3500, 4800, comercios)));
    }

    private static AgregadoZonaProducto producto(String nombre, int minimo, int maximo, int comercios) {
        return new AgregadoZonaProducto(nombre, comercios, comercios, new BigDecimal(minimo), new BigDecimal("4200"),
                new BigDecimal(maximo), new BigDecimal("20"));
    }
}
