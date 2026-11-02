package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReglaPrecioZonaTest {

    private static final UUID AZUCAR_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final String NOMBRE = "Azúcar Ledesma 1 kg";

    private final ReglaPrecioZona regla = new ReglaPrecioZona();

    @Test
    @DisplayName("Es la regla de los avisos de precio frente a la zona")
    void tipoShouldBePrecioZona() {
        assertThat(regla.tipo()).isEqualTo(TipoAviso.PRECIO_ZONA);
    }

    @Test
    @DisplayName("Avisa del precio por debajo del rango de la zona y sugiere revisar costos y margen")
    void evaluarShouldWarnWhenPriceIsBelowZoneRange() {
        List<ResultadoRegla> resultados = whenEvaluar(zona(10, 10), azucar(800, 700));

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.entidad()).isEqualTo("registro:" + AZUCAR_ID);
            assertThat(resultado.prioridad()).isEqualTo((short) 2);
            assertThat(resultado.proyectoId()).isEqualTo(AvisoTestData.PROYECTO_ID);
            assertThat(resultado.titulo()).isEqualTo("Azúcar Ledesma 1 kg está más barato que en tu zona");
            assertThat(resultado.detalle()).isEqualTo("Lo tenés a $800 y en tu zona se vende entre $1.000 y $1.300: "
                    + "$200 (20 %) menos que el mínimo. "
                    + "Revisá tus costos y tu margen: puede que estés vendiendo más barato de lo necesario.");
            assertThat(resultado.motivos()).containsExactly(
                    "En tu zona, 10 comercios lo venden entre $1.000 y $1.300.",
                    "Con tu costo de $700, te quedan $100 por unidad (14 % sobre el costo).");
            assertThat(resultado.acciones()).hasSize(1);
            assertThat(accion(resultado, 0)).containsEntry("tipo", "ABRIR").containsEntry("texto", "Ver producto")
                    .containsEntry("registroId", AZUCAR_ID.toString());
            assertThat(resultado.datos()).containsEntry("precio", new BigDecimal("800"))
                    .containsEntry("minimo", new BigDecimal("1000")).containsEntry("maximo", new BigDecimal("1300"));
        });
    }

    @Test
    @DisplayName("Cuando el costo supera al precio dice cuánto pierde por unidad")
    void evaluarShouldMentionLossWhenCostIsAbovePrice() {
        List<ResultadoRegla> resultados = whenEvaluar(zona(10, 10), azucar(800, 900));

        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.motivos()).contains("Con tu costo de $900, perdés $100 por unidad."));
    }

    @Test
    @DisplayName("Sin costo cargado lo avisa en el motivo")
    void evaluarShouldAskForCostWhenProductHasNoCost() {
        assertThat(whenEvaluar(zona(10, 10), azucar(800, null))).singleElement().satisfies(resultado ->
                assertThat(motivo(resultado, 1)).startsWith("Todavía no cargaste el costo"));
        assertThat(whenEvaluar(zona(10, 10), azucar(800, 0))).singleElement().satisfies(resultado ->
                assertThat(motivo(resultado, 1)).startsWith("Todavía no cargaste el costo"));
    }

    @Test
    @DisplayName("Avisa del precio por encima del rango y sugiere bajarlo al máximo de la zona")
    void evaluarShouldWarnWhenPriceIsAboveZoneRange() {
        List<ResultadoRegla> resultados = whenEvaluar(zona(10, 10), azucar(1500, null));

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.titulo()).isEqualTo("Azúcar Ledesma 1 kg está más caro que en tu zona");
            assertThat(resultado.detalle()).isEqualTo("Lo tenés a $1.500 y en tu zona se vende entre $1.000 y $1.300: "
                    + "$200 (15 %) más que el máximo. Bajarlo a $1.300 lo deja dentro del rango y evita que pierdas ventas.");
            assertThat(resultado.motivos()).containsExactly(
                    "En tu zona, 10 comercios lo venden entre $1.000 y $1.300.",
                    "Los clientes comparan: por encima de lo que cobran al lado se compra menos.");
            assertThat(resultado.acciones()).hasSize(2);
            assertThat(resultado.acciones().get(0)).isEqualTo(Map.of(
                    "tipo", "EDITAR_REGISTRO",
                    "texto", "Bajar a $1.300",
                    "proyectoId", AvisoTestData.PROYECTO_ID.toString(),
                    "bloqueId", AvisoTestData.INVENTARIO_ID.toString(),
                    "registroId", AZUCAR_ID.toString(),
                    "valores", Map.of("price", new BigDecimal("1300"))));
            assertThat(accion(resultado, 1)).containsEntry("tipo", "ABRIR");
        });
    }

    @Test
    @DisplayName("Si redondear el máximo a la decena no deja el precio por debajo del actual, sugiere el máximo exacto")
    void evaluarShouldSuggestExactMaximumWhenRoundingDoesNotLowerThePrice() {
        AnalisisZona zona = zonaConRango("1000", "1296", 10);

        List<ResultadoRegla> resultados = whenEvaluar(zona, azucar(1298, null));

        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(accion(resultado, 0)).containsEntry("texto", "Bajar a $1.296")
                        .containsEntry("valores", Map.of("price", new BigDecimal("1296"))));
    }

    @Test
    @DisplayName("No avisa si el precio está dentro del rango, incluidos los extremos")
    void evaluarShouldReturnNothingWhenPriceIsInsideRange() {
        assertThat(whenEvaluar(zona(10, 10), azucar(1150, null))).isEmpty();
        assertThat(whenEvaluar(zona(10, 10), azucar(1000, null))).isEmpty();
        assertThat(whenEvaluar(zona(10, 10), azucar(1300, null))).isEmpty();
    }

    @Test
    @DisplayName("Con menos de 3 comercios en la zona no calcula ni muestra rango")
    void evaluarShouldReturnNothingWhenZoneHasFewerThanThreeStores() {
        assertThat(whenEvaluar(zona(2, 10), azucar(800, null))).isEmpty();
        assertThat(whenEvaluar(zona(2, 10), azucar(1500, null))).isEmpty();
    }

    @Test
    @DisplayName("Con exactamente 3 comercios sí avisa")
    void evaluarShouldWarnWhenZoneHasExactlyThreeStores() {
        assertThat(whenEvaluar(zona(3, 3), azucar(800, null))).hasSize(1);
    }

    @Test
    @DisplayName("Con menos de 3 comercios que cobran el producto no compara contra él")
    void evaluarShouldReturnNothingWhenProductHasFewerThanThreeStores() {
        assertThat(whenEvaluar(zona(10, 2), azucar(800, null))).isEmpty();
    }

    @Test
    @DisplayName("Sin zona no avisa")
    void evaluarShouldReturnNothingWithoutZone() {
        assertThat(whenEvaluar(AnalisisZona.sinZona(), azucar(800, null))).isEmpty();
    }

    @Test
    @DisplayName("No avisa si el producto no está en la zona")
    void evaluarShouldReturnNothingWhenProductIsNotInZone() {
        assertThat(whenEvaluar(zonaConProducto("Yerba Playadito 1 kg", 10), azucar(800, null))).isEmpty();
    }

    @Test
    @DisplayName("Reconoce el producto de la zona aunque cambien las tildes y las mayúsculas")
    void evaluarShouldMatchZoneProductIgnoringAccentsAndCase() {
        assertThat(whenEvaluar(zonaConProducto("AZUCAR LEDESMA 1 KG", 10), azucar(800, null))).hasSize(1);
    }

    @Test
    @DisplayName("No avisa de productos sin precio o con precio en cero")
    void evaluarShouldReturnNothingWhenProductHasNoPrice() {
        assertThat(whenEvaluar(zona(10, 10), azucar(null, null))).isEmpty();
        assertThat(whenEvaluar(zona(10, 10), azucar(0, null))).isEmpty();
    }

    @Test
    @DisplayName("Avisa aunque el producto no tenga stock: el precio se compara igual")
    void evaluarShouldWarnEvenWhenProductHasNoStock() {
        List<ResultadoRegla> resultados = whenEvaluar(zona(10, 10),
                AvisoTestData.producto(AZUCAR_ID, NOMBRE, 0, 800, null));

        assertThat(resultados).hasSize(1);
    }

    @Test
    @DisplayName("Nombra al producto sin nombre con un texto genérico")
    void evaluarShouldUseGenericNameWhenProductHasNoName() {
        AnalisisZona zona = zonaConProducto("Un producto sin nombre", 10);

        List<ResultadoRegla> resultados = whenEvaluar(zona,
                AvisoTestData.producto(AZUCAR_ID, null, 5, 800, null));

        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.titulo()).isEqualTo("Un producto sin nombre está más barato que en tu zona"));
    }

    @Test
    @DisplayName("Sólo muestra el rango agregado de la zona, sin datos de ningún comercio")
    void evaluarShouldOnlyExposeAggregatedRange() {
        ResultadoRegla resultado = whenEvaluar(zona(10, 10), azucar(800, 700)).get(0);

        assertThat(resultado.datos().keySet()).containsExactly("precio", "minimo", "maximo");
        assertThat(resultado.detalle() + resultado.motivos()).doesNotContain("promedio");
    }

    @Test
    @DisplayName("La huella cambia cuando cambia el precio y también cuando cambia el rango de la zona")
    void evaluarShouldChangeHuellaWhenPriceOrRangeChanges() {
        ResultadoRegla base = whenEvaluar(zona(10, 10), azucar(800, null)).get(0);
        ResultadoRegla otroPrecio = whenEvaluar(zona(10, 10), azucar(850, null)).get(0);
        ResultadoRegla otroRango = whenEvaluar(zonaConRango("1100", "1300", 10), azucar(800, null)).get(0);
        ResultadoRegla igual = whenEvaluar(zona(10, 10), azucar(800, null)).get(0);

        assertThat(otroPrecio.huellaPara(TipoAviso.PRECIO_ZONA)).isNotEqualTo(base.huellaPara(TipoAviso.PRECIO_ZONA));
        assertThat(otroRango.huellaPara(TipoAviso.PRECIO_ZONA)).isNotEqualTo(base.huellaPara(TipoAviso.PRECIO_ZONA));
        assertThat(igual.huellaPara(TipoAviso.PRECIO_ZONA)).isEqualTo(base.huellaPara(TipoAviso.PRECIO_ZONA));
    }

    // --- act ---
    private List<ResultadoRegla> whenEvaluar(AnalisisZona zona, Registro... productos) {
        BloqueInventario inventario = AvisoTestData.inventarioConPrecios(productos);
        return regla.evaluar(new ContextoNegocio(AvisoTestData.NEGOCIO_ID, null, List.of(inventario), List.of(), zona));
    }

    // --- helpers ---
    @SuppressWarnings("unchecked")
    private static Map<String, Object> accion(ResultadoRegla resultado, int indice) {
        return (Map<String, Object>) resultado.acciones().get(indice);
    }

    private static String motivo(ResultadoRegla resultado, int indice) {
        return resultado.motivos().get(indice).toString();
    }

    private static Registro azucar(Object precio, Object costo) {
        return AvisoTestData.producto(AZUCAR_ID, NOMBRE, 25, precio, costo);
    }

    /** Rango de $1.000 a $1.300 con los comercios de la zona y los que cobran el producto que se indican. */
    private static AnalisisZona zona(long comerciosZona, int comerciosProducto) {
        return new AnalisisZona("San Justo", comerciosZona, List.of(producto(NOMBRE, "1000", "1300", comerciosProducto)));
    }

    private static AnalisisZona zonaConRango(String minimo, String maximo, int comerciosProducto) {
        return new AnalisisZona("San Justo", 10, List.of(producto(NOMBRE, minimo, maximo, comerciosProducto)));
    }

    private static AnalisisZona zonaConProducto(String nombre, int comerciosProducto) {
        return new AnalisisZona("San Justo", 10, List.of(producto(nombre, "1000", "1300", comerciosProducto)));
    }

    private static AgregadoZonaProducto producto(String nombre, String minimo, String maximo, int comercios) {
        return new AgregadoZonaProducto(nombre, comercios, comercios, new BigDecimal(minimo), new BigDecimal("1147"),
                new BigDecimal(maximo), new BigDecimal("3"));
    }
}
