package com.ensambla.backend.domain.service.aviso.regla;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.ContextoNegocio;
import com.ensambla.backend.domain.model.aviso.ResultadoRegla;
import com.ensambla.backend.domain.model.enums.TipoAviso;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.service.aviso.MetricasVentasService;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReglaProductoEstancadoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final UUID AZUCAR_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID YERBA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID COCA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000003");
    private static final String NOMBRE = "Azúcar Ledesma 1 kg";

    private final ReglaProductoEstancado regla = new ReglaProductoEstancado(
            new MetricasVentasService(30, new BigDecimal("0.5")));

    @Test
    @DisplayName("Es la regla de los avisos de producto estancado")
    void tipoShouldBeEstancado() {
        assertThat(regla.tipo()).isEqualTo(TipoAviso.ESTANCADO);
    }

    @Test
    @DisplayName("Avisa del producto caro que no se vende y sugiere el precio promedio al que lo vendía")
    void evaluarShouldSuggestHistoricPriceWhenProductIsExpensiveAndNotSelling() {
        List<Registro> ventas = ventasDelResto();
        ventas.add(venta(AZUCAR_ID, 2, 1000, "2026-08-01"));

        List<ResultadoRegla> resultados = whenEvaluar(sinZona(), ventas, azucar(25, 1500));

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.entidad()).isEqualTo("registro:" + AZUCAR_ID);
            assertThat(resultado.prioridad()).isEqualTo((short) 2);
            assertThat(resultado.titulo()).isEqualTo("Azúcar Ledesma 1 kg está caro y no se mueve");
            assertThat(resultado.detalle()).isEqualTo("No vendiste ninguno en los últimos 30 días y lo tenés a $1.500. "
                    + "Si lo bajás a $1.000, queda cerca de lo que solías cobrar.");
            assertThat(resultado.motivos()).containsExactly(
                    "Está por encima de tu precio promedio histórico de $1.000.",
                    "La última venta fue el 1/8.",
                    "Tenés 25 en stock.");
            assertThat(resultado.proyectoId()).isEqualTo(AvisoTestData.PROYECTO_ID);
        });
    }

    @Test
    @DisplayName("Ofrece rebajar el precio con una acción que edita el registro y otra que abre el producto")
    void evaluarShouldOfferRebajarAndAbrirActions() {
        List<Registro> ventas = ventasDelResto();
        ventas.add(venta(AZUCAR_ID, 2, 1000, "2026-08-01"));

        List<ResultadoRegla> resultados = whenEvaluar(sinZona(), ventas, azucar(25, 1500));

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.acciones()).hasSize(2);
            assertThat(resultado.acciones().get(0)).isEqualTo(Map.of(
                    "tipo", "EDITAR_REGISTRO",
                    "texto", "Rebajar a $1.000",
                    "proyectoId", AvisoTestData.PROYECTO_ID.toString(),
                    "bloqueId", AvisoTestData.INVENTARIO_ID.toString(),
                    "registroId", AZUCAR_ID.toString(),
                    "valores", Map.of("price", new BigDecimal("1000"))));
            assertThat(resultado.acciones().get(1)).isEqualTo(Map.of(
                    "tipo", "ABRIR",
                    "texto", "Ver producto",
                    "proyectoId", AvisoTestData.PROYECTO_ID.toString(),
                    "bloqueId", AvisoTestData.INVENTARIO_ID.toString(),
                    "registroId", AZUCAR_ID.toString()));
        });
    }

    @Test
    @DisplayName("Con ventas en el período lo cuenta en el detalle")
    void evaluarShouldMentionUnitsSoldInThePeriod() {
        List<Registro> ventas = ventasDelResto();
        ventas.add(venta(AZUCAR_ID, 3, 1000, "2026-10-10"));

        List<ResultadoRegla> resultados = whenEvaluar(sinZona(), ventas, azucar(25, 1500));

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.detalle()).startsWith("Vendiste 3 en los últimos 30 días y lo tenés a $1.500.");
            assertThat(resultado.motivos()).contains("La última venta fue el 10/10.");
        });
    }

    @Test
    @DisplayName("Avisa del producto más caro que la zona y sugiere el promedio de la zona redondeado a la decena")
    void evaluarShouldSuggestZonePriceWhenProductIsExpensiveComparedToZone() {
        List<ResultadoRegla> resultados = whenEvaluar(zonaConAzucar(), ventasDelResto(), azucar(25, 1500));

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.detalle()).isEqualTo("No vendiste ninguno en los últimos 30 días y lo tenés a $1.500. "
                    + "Si lo bajás a $1.150, queda cerca de lo que cobran en tu zona.");
            assertThat(resultado.motivos()).containsExactly(
                    "En tu zona, 10 comercios lo venden entre $1.000 y $1.300.",
                    "Todavía no anotaste ventas de este producto.",
                    "Tenés 25 en stock.");
            assertThat(resultado.datos()).containsEntry("sugerido", new BigDecimal("1150"));
        });
    }

    @Test
    @DisplayName("Prefiere la referencia de la zona cuando el historial propio también dice que está caro")
    void evaluarShouldPreferZoneReferenceOverHistoricPrice() {
        List<Registro> ventas = ventasDelResto();
        ventas.add(venta(AZUCAR_ID, 2, 1000, "2026-08-01"));

        List<ResultadoRegla> resultados = whenEvaluar(zonaConAzucar(), ventas, azucar(25, 1500));

        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.motivos().get(0).toString()).startsWith("En tu zona"));
    }

    @Test
    @DisplayName("Usa el historial propio cuando el precio no supera al de la zona")
    void evaluarShouldFallBackToHistoricPriceWhenProductIsNotExpensiveComparedToZone() {
        List<Registro> ventas = ventasDelResto();
        ventas.add(venta(AZUCAR_ID, 2, 1000, "2026-08-01"));

        List<ResultadoRegla> resultados = whenEvaluar(zonaConAzucar("Azúcar Ledesma 1 kg", "1450"), ventas,
                azucar(25, 1500));

        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.motivos().get(0).toString()).startsWith("Está por encima de tu precio promedio histórico"));
    }

    @Test
    @DisplayName("Reconoce el producto de la zona aunque cambien las tildes y las mayúsculas")
    void evaluarShouldMatchZoneProductIgnoringAccentsAndCase() {
        List<ResultadoRegla> resultados = whenEvaluar(zonaConAzucar("AZUCAR LEDESMA 1 KG", "1147"),
                ventasDelResto(), azucar(25, 1500));

        assertThat(resultados).hasSize(1);
    }

    @Test
    @DisplayName("No avisa de un producto caro que se vende normalmente")
    void evaluarShouldReturnNothingWhenExpensiveProductSellsNormally() {
        List<Registro> ventas = ventasDelResto();
        ventas.add(venta(AZUCAR_ID, 20, 1500, "2026-10-10"));

        List<ResultadoRegla> resultados = whenEvaluar(zonaConAzucar(), ventas, azucar(25, 1500));

        assertThat(resultados).isEmpty();
    }

    @Test
    @DisplayName("No avisa nada si el negocio no anotó ventas")
    void evaluarShouldReturnNothingWhenNegocioHasNoSales() {
        List<ResultadoRegla> resultados = whenEvaluar(zonaConAzucar(), List.of(), azucar(25, 1500));

        assertThat(resultados).isEmpty();
    }

    @Test
    @DisplayName("No avisa nada sin la fecha de hoy")
    void evaluarShouldReturnNothingWhenHoyIsMissing() {
        ContextoNegocio contexto = new ContextoNegocio(AvisoTestData.NEGOCIO_ID,
                List.of(AvisoTestData.inventarioConPrecios(azucar(25, 1500))));

        assertThat(regla.evaluar(contexto)).isEmpty();
    }

    @Test
    @DisplayName("No avisa si el precio no supera en más de un 5 % al de referencia")
    void evaluarShouldReturnNothingWhenPriceIsWithinMargin() {
        List<Registro> ventas = ventasDelResto();
        ventas.add(venta(AZUCAR_ID, 2, 1500, "2026-08-01"));

        List<ResultadoRegla> resultados = whenEvaluar(sinZona(), ventas, azucar(25, 1575));

        assertThat(resultados).isEmpty();
    }

    @Test
    @DisplayName("No avisa si el producto no está en la zona ni tiene precio histórico")
    void evaluarShouldReturnNothingWhenThereIsNoReference() {
        List<ResultadoRegla> resultados = whenEvaluar(zonaConAzucar("Yerba Playadito 1 kg", "1000"),
                ventasDelResto(), azucar(25, 1500));

        assertThat(resultados).isEmpty();
    }

    @Test
    @DisplayName("No avisa si el precio sugerido redondeado no queda por debajo del actual")
    void evaluarShouldReturnNothingWhenSuggestedPriceIsNotLowerThanCurrent() {
        List<ResultadoRegla> resultados = whenEvaluar(zonaConAzucar(NOMBRE, "15"), ventasDelResto(),
                azucar(25, 16));

        // 16 supera en más de 5 % a 15, pero 15 redondeado a la decena es 20
        assertThat(resultados).isEmpty();
    }

    @Test
    @DisplayName("No avisa si el precio sugerido redondeado queda en cero")
    void evaluarShouldReturnNothingWhenSuggestedPriceRoundsToZero() {
        List<ResultadoRegla> resultados = whenEvaluar(zonaConAzucar(NOMBRE, "4"), ventasDelResto(),
                azucar(25, 5));

        assertThat(resultados).isEmpty();
    }

    @Test
    @DisplayName("No avisa de productos sin stock")
    void evaluarShouldReturnNothingWhenProductHasNoStock() {
        assertThat(whenEvaluar(zonaConAzucar(), ventasDelResto(), azucar(0, 1500))).isEmpty();
    }

    @Test
    @DisplayName("No avisa de productos sin cantidad cargada")
    void evaluarShouldReturnNothingWhenProductHasNoQuantity() {
        assertThat(whenEvaluar(zonaConAzucar(), ventasDelResto(), azucar(null, 1500))).isEmpty();
    }

    @Test
    @DisplayName("No avisa de productos sin precio")
    void evaluarShouldReturnNothingWhenProductHasNoPrice() {
        assertThat(whenEvaluar(zonaConAzucar(), ventasDelResto(), azucar(25, null))).isEmpty();
    }

    @Test
    @DisplayName("No avisa de productos con precio en cero")
    void evaluarShouldReturnNothingWhenProductPriceIsZero() {
        assertThat(whenEvaluar(zonaConAzucar(), ventasDelResto(), azucar(25, 0))).isEmpty();
    }

    @Test
    @DisplayName("Nombra al producto sin nombre y al de nombre en blanco con un texto genérico")
    void evaluarShouldUseGenericNameWhenProductHasNoName() {
        List<Registro> ventas = ventasDelResto();
        ventas.add(venta(AZUCAR_ID, 2, 1000, "2026-08-01"));

        List<ResultadoRegla> sinNombre = whenEvaluar(sinZona(), ventas,
                AvisoTestData.producto(AZUCAR_ID, null, 25, 1500, null));
        List<ResultadoRegla> enBlanco = whenEvaluar(sinZona(), ventas,
                AvisoTestData.producto(AZUCAR_ID, "   ", 25, 1500, null));

        assertThat(sinNombre).singleElement().satisfies(resultado ->
                assertThat(resultado.titulo()).isEqualTo("Un producto sin nombre está caro y no se mueve"));
        assertThat(enBlanco).singleElement().satisfies(resultado ->
                assertThat(resultado.titulo()).isEqualTo("Un producto sin nombre está caro y no se mueve"));
    }

    @Test
    @DisplayName("Escribe los precios con decimales con coma")
    void evaluarShouldFormatDecimalPricesWithComma() {
        List<Registro> ventas = ventasDelResto();
        ventas.add(venta(AZUCAR_ID, 2, 1000, "2026-08-01"));

        List<ResultadoRegla> resultados = whenEvaluar(sinZona(), ventas, azucar(25, 1500.5));

        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.detalle()).contains("lo tenés a $1.500,5."));
    }

    @Test
    @DisplayName("La huella no cambia cuando sólo cambian las ventas")
    void evaluarShouldKeepHuellaWhenOnlySalesChange() {
        List<Registro> sinVentasDelProducto = ventasDelResto();
        List<Registro> conVentaVieja = ventasDelResto();
        conVentaVieja.add(venta(AZUCAR_ID, 2, 1150, "2026-08-01"));

        ResultadoRegla antes = whenEvaluar(zonaConAzucar(), sinVentasDelProducto, azucar(25, 1500)).get(0);
        ResultadoRegla despues = whenEvaluar(zonaConAzucar(), conVentaVieja, azucar(25, 1500)).get(0);

        assertThat(despues.huellaPara(TipoAviso.ESTANCADO)).isEqualTo(antes.huellaPara(TipoAviso.ESTANCADO));
    }

    @Test
    @DisplayName("La huella cambia cuando cambia el precio del producto")
    void evaluarShouldChangeHuellaWhenPriceChanges() {
        ResultadoRegla antes = whenEvaluar(zonaConAzucar(), ventasDelResto(), azucar(25, 1500)).get(0);
        ResultadoRegla despues = whenEvaluar(zonaConAzucar(), ventasDelResto(), azucar(25, 1600)).get(0);

        assertThat(despues.huellaPara(TipoAviso.ESTANCADO)).isNotEqualTo(antes.huellaPara(TipoAviso.ESTANCADO));
    }

    // --- act ---
    private List<ResultadoRegla> whenEvaluar(AnalisisZona zona, List<Registro> ventas, Registro... productos) {
        BloqueInventario inventario = AvisoTestData.inventarioConPrecios(productos);
        return regla.evaluar(new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY, List.of(inventario),
                List.of(AvisoTestData.ventas(ventas.toArray(new Registro[0]))), zona));
    }

    // --- helpers ---
    private static Registro azucar(Object cantidad, Object precio) {
        return AvisoTestData.producto(AZUCAR_ID, NOMBRE, cantidad, precio, null);
    }

    /** La yerba vende 10 y la gaseosa 20 en el período: la mediana de los demás es 15. */
    private static List<Registro> ventasDelResto() {
        List<Registro> ventas = new ArrayList<>();
        ventas.add(venta(YERBA_ID, 10, 3900, "2026-10-12"));
        ventas.add(venta(COCA_ID, 20, 2750, "2026-10-14"));
        return ventas;
    }

    private static Registro venta(UUID producto, int cantidad, int precio, String fecha) {
        return AvisoTestData.venta(producto.toString(), cantidad, precio, fecha, null);
    }

    private static AnalisisZona sinZona() {
        return AnalisisZona.sinZona();
    }

    private static AnalisisZona zonaConAzucar() {
        return zonaConAzucar(NOMBRE, "1147");
    }

    private static AnalisisZona zonaConAzucar(String nombre, String promedio) {
        AgregadoZonaProducto producto = new AgregadoZonaProducto(nombre, 10, 9, new BigDecimal("1000"),
                new BigDecimal(promedio), new BigDecimal("1300"), new BigDecimal("3"));
        return new AnalisisZona("San Justo", 10, List.of(producto));
    }
}
