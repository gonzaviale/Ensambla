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

class ReglaProductoDeLaZonaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final String CAFE = "Café La Virginia 250 g";
    private static final String JABON = "Jabón en polvo Skip 800 g";
    private static final String YERBA = "Yerba Nobleza Gaucha 1 kg";
    private static final String AZUCAR = "Azúcar Ledesma 1 kg";
    private static final String FIDEOS = "Fideos Luchetti 500 g";

    private final ReglaProductoDeLaZona regla = new ReglaProductoDeLaZona(new BigDecimal("0.6"), 2);

    @Test
    @DisplayName("Es la regla de los avisos de consumo de la zona")
    void tipoShouldBeConsumoZona() {
        assertThat(regla.tipo()).isEqualTo(TipoAviso.CONSUMO_ZONA);
    }

    @Test
    @DisplayName("Sugiere los productos más vendidos de la zona que el negocio no tiene, y no los que ya tiene")
    void evaluarShouldSuggestZoneBestSellersThatTheNegocioLacks() {
        List<ResultadoRegla> resultados = whenEvaluar(zona(), producto(YERBA), producto(FIDEOS));

        assertThat(resultados).extracting(ResultadoRegla::titulo).containsExactly(
                "En tu zona se vende Café La Virginia 250 g y no lo tenés",
                "En tu zona se vende Jabón en polvo Skip 800 g y no lo tenés");
    }

    @Test
    @DisplayName("Cuenta con sólo los agregados de la zona, sin identificar a ningún comercio")
    void evaluarShouldDescribeTheProductWithAggregatesOnly() {
        List<ResultadoRegla> resultados = whenEvaluar(zona(), producto(YERBA));

        assertThat(resultados.get(0)).satisfies(resultado -> {
            assertThat(resultado.entidad()).isEqualTo(ResultadoRegla.entidadDeZona(CAFE));
            assertThat(resultado.prioridad()).isEqualTo((short) 1);
            assertThat(resultado.proyectoId()).isEqualTo(AvisoTestData.PROYECTO_ID);
            assertThat(resultado.detalle()).isEqualTo("8 de 10 comercios lo venden, a $3.200 en promedio. "
                    + "Cada uno vende unas 9 unidades por semana.");
            assertThat(resultado.motivos()).containsExactly(
                    "Es un dato agregado de varios comercios de tu zona: nadie queda identificado.",
                    "Lo cruzamos con tu inventario y no encontramos nada parecido.");
            assertThat(resultado.datos()).containsOnlyKeys("nombre", "precio")
                    .containsEntry("nombre", CAFE).containsEntry("precio", new BigDecimal("3200"));
        });
    }

    @Test
    @DisplayName("Ofrece sumarlo al inventario y ver el producto en la zona")
    void evaluarShouldOfferCrearRegistroAndVerZonaActions() {
        List<ResultadoRegla> resultados = whenEvaluar(zona(), producto(YERBA));

        assertThat(resultados.get(0).acciones()).hasSize(2);
        assertThat(resultados.get(0).acciones().get(0)).isEqualTo(Map.of(
                "tipo", "CREAR_REGISTRO",
                "texto", "Sumar a mi inventario",
                "proyectoId", AvisoTestData.PROYECTO_ID.toString(),
                "bloqueId", AvisoTestData.INVENTARIO_ID.toString(),
                "valores", Map.of("name", CAFE, "quantity", 0, "price", new BigDecimal("3200"), "minimum", 5)));
        assertThat(resultados.get(0).acciones().get(1)).isEqualTo(Map.of(
                "tipo", "VER_ZONA", "texto", "Ver en Mi zona", "producto", CAFE));
    }

    @Test
    @DisplayName("Sugiere como precio el promedio de la zona redondeado a la decena")
    void evaluarShouldRoundSuggestedPriceToTen() {
        AnalisisZona zona = zonaCon(10, agregado(CAFE, 8, "3147", "9"));

        List<ResultadoRegla> resultados = whenEvaluar(zona, producto(YERBA));

        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.datos()).containsEntry("precio", new BigDecimal("3150")));
    }

    @Test
    @DisplayName("Avisa de a lo sumo el máximo de productos, los más vendidos primero")
    void evaluarShouldLimitToMaximumBestSellersFirst() {
        AnalisisZona zona = zonaCon(10,
                agregado(AZUCAR, 7, "1150", "3"), agregado(JABON, 7, "3500", "6"),
                agregado(CAFE, 8, "3200", "9"), agregado(YERBA, 9, "4200", "20"));

        List<ResultadoRegla> resultados = whenEvaluar(zona, producto(FIDEOS));

        assertThat(resultados).extracting(ResultadoRegla::titulo).containsExactly(
                "En tu zona se vende Yerba Nobleza Gaucha 1 kg y no lo tenés",
                "En tu zona se vende Café La Virginia 250 g y no lo tenés");
    }

    @Test
    @DisplayName("Reconoce el mismo producto aunque cambien la marca, el tamaño o las palabras de relleno")
    void evaluarShouldRecognizeTheSameProductWithDifferentBrandOrSize() {
        assertThat(whenEvaluar(zonaCon(10, agregado(CAFE, 8, "3200", "9")), producto("Café Cabrales 500 g"))).isEmpty();
        assertThat(whenEvaluar(zonaCon(10, agregado(CAFE, 8, "3200", "9")), producto("Con Café Cabrales"))).isEmpty();
        assertThat(whenEvaluar(zonaCon(10, agregado(CAFE, 8, "3200", "9")), producto("CAFÉ"))).isEmpty();
    }

    @Test
    @DisplayName("Mira los productos de todas las tablas de inventario del negocio")
    void evaluarShouldLookAtEveryInventoryTable() {
        BloqueInventario primero = AvisoTestData.inventarioConPrecios(producto(FIDEOS));
        BloqueInventario segundo = AvisoTestData.inventarioConPrecios(producto("Café Cabrales 500 g"));

        List<ResultadoRegla> resultados = regla.evaluar(new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY,
                List.of(primero, segundo), List.of(), zona()));

        assertThat(resultados).extracting(ResultadoRegla::titulo).doesNotContain(
                "En tu zona se vende Café La Virginia 250 g y no lo tenés");
    }

    @Test
    @DisplayName("Un producto sin nombre, en blanco o sin palabras que lo distingan no tapa a ninguno de la zona")
    void evaluarShouldIgnoreProductsWithoutDistinctiveName() {
        List<ResultadoRegla> resultados = whenEvaluar(zonaCon(10, agregado(CAFE, 8, "3200", "9")),
                AvisoTestData.producto(UUID.randomUUID(), null, 5, 100, null),
                producto("   "), producto("500 g"));

        assertThat(resultados).hasSize(1);
    }

    @Test
    @DisplayName("No avisa de lo que vende menos del 60 % de los comercios de la zona, pero sí del 60 % justo")
    void evaluarShouldRequireMinimumPopularity() {
        assertThat(whenEvaluar(zonaCon(10, agregado(CAFE, 5, "3200", "9")), producto(YERBA))).isEmpty();
        assertThat(whenEvaluar(zonaCon(10, agregado(CAFE, 6, "3200", "9")), producto(YERBA))).hasSize(1);
    }

    @Test
    @DisplayName("Sin ranking no avisa: con menos de 3 comercios en la zona o que lo vendan, o sin zona")
    void evaluarShouldReturnNothingWithoutAnonymousRanking() {
        assertThat(whenEvaluar(zonaCon(2, agregado(CAFE, 3, "3200", "9")), producto(YERBA))).isEmpty();
        assertThat(whenEvaluar(zonaCon(3, agregado(CAFE, 2, "3200", "9")), producto(YERBA))).isEmpty();
        assertThat(whenEvaluar(AnalisisZona.sinZona(), producto(YERBA))).isEmpty();
    }

    @Test
    @DisplayName("Sin tabla de inventario donde sumarlo no avisa")
    void evaluarShouldReturnNothingWithoutInventory() {
        ContextoNegocio sinInventario = new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY, List.of(), List.of(), zona());

        assertThat(regla.evaluar(sinInventario)).isEmpty();
    }

    @Test
    @DisplayName("La huella no cambia con los comercios ni las unidades, pero sí con el precio sugerido")
    void evaluarShouldKeepHuellaWhenOnlyCountsChangeAndChangeItWhenPriceChanges() {
        ResultadoRegla base = whenEvaluar(zonaCon(10, agregado(CAFE, 8, "3200", "9")), producto(YERBA)).get(0);
        ResultadoRegla masComercios = whenEvaluar(zonaCon(12, agregado(CAFE, 10, "3200", "15")), producto(YERBA)).get(0);
        ResultadoRegla otroPrecio = whenEvaluar(zonaCon(10, agregado(CAFE, 8, "3400", "9")), producto(YERBA)).get(0);

        assertThat(masComercios.huellaPara(TipoAviso.CONSUMO_ZONA)).isEqualTo(base.huellaPara(TipoAviso.CONSUMO_ZONA));
        assertThat(otroPrecio.huellaPara(TipoAviso.CONSUMO_ZONA)).isNotEqualTo(base.huellaPara(TipoAviso.CONSUMO_ZONA));
    }

    // --- act ---
    private List<ResultadoRegla> whenEvaluar(AnalisisZona zona, Registro... productos) {
        BloqueInventario inventario = AvisoTestData.inventarioConPrecios(productos);
        return regla.evaluar(new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY, List.of(inventario), List.of(), zona));
    }

    // --- helpers ---
    private static Registro producto(String nombre) {
        return AvisoTestData.producto(UUID.randomUUID(), nombre, 10, 1000, null);
    }

    /** Zona de 10 comercios: la yerba la vende 9, el café 8, el jabón 7, los fideos 5 (50 %) y la sal 2. */
    private static AnalisisZona zona() {
        return zonaCon(10,
                agregado(FIDEOS, 5, "1300", "12"), agregado(JABON, 7, "3500", "6"),
                agregado(CAFE, 8, "3200", "9"), agregado(YERBA, 9, "4200", "20"),
                agregado("Sal Celusal 500 g", 2, "900", "30"));
    }

    private static AnalisisZona zonaCon(long comercios, AgregadoZonaProducto... productos) {
        return new AnalisisZona("San Justo", comercios, List.of(productos));
    }

    private static AgregadoZonaProducto agregado(String nombre, int comerciosVenden, String promedio, String semana) {
        return new AgregadoZonaProducto(nombre, 8, comerciosVenden, new BigDecimal("100"), new BigDecimal(promedio),
                new BigDecimal("9999"), new BigDecimal(semana));
    }
}
