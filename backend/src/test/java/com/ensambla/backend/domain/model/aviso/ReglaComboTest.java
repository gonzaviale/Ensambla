package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReglaComboTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final UUID ALFAJOR_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID GALLETITAS_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID YERBA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000003");
    private static final UUID COCA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000004");
    private static final String ALFAJOR = "Alfajor Jorgito";
    private static final String GALLETITAS = "Galletitas Criollitas 3 U";
    private static final List<String> DIAS = List.of("2026-10-03", "2026-10-09", "2026-10-15", "2026-10-17");

    private final ReglaCombo regla = new ReglaCombo(
            new MetricasVentas(30, new BigDecimal("0.5")), new CanastasVentas(30, 3));

    @Test
    @DisplayName("Es la regla de los avisos de combos")
    void tipoShouldBeCombo() {
        assertThat(regla.tipo()).isEqualTo(TipoAviso.COMBO);
    }

    @Test
    @DisplayName("Sugiere el combo de dos productos que salen poco y se llevan juntos, con un 10 % menos")
    void evaluarShouldSuggestComboForLowRotationProductsSoldTogether() {
        List<ResultadoRegla> resultados = whenEvaluar(ventasConElResto(1, 1), alfajor(), galletitas());

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.entidad()).isEqualTo("combo:" + ALFAJOR_ID + ":" + GALLETITAS_ID);
            assertThat(resultado.prioridad()).isEqualTo((short) 1);
            assertThat(resultado.titulo())
                    .isEqualTo("Alfajor Jorgito y Galletitas Criollitas 3 U se venden juntos");
            assertThat(resultado.detalle()).isEqualTo(
                    "Salen poco por separado, pero 4 veces se los llevó el mismo cliente el mismo día.");
            assertThat(resultado.motivos()).containsExactly(
                    "Por separado suman $2.050; como combo, $1.850 (10 % menos) puede ayudar a que roten.",
                    "El combo se crea como un producto nuevo del inventario, con las unidades que podés armar hoy con tu stock.");
            assertThat(resultado.proyectoId()).isEqualTo(AvisoTestData.PROYECTO_ID);
        });
    }

    @Test
    @DisplayName("Ofrece crear el combo como un producto nuevo del inventario con precio y costo")
    void evaluarShouldOfferCreatingTheComboWithPriceAndCost() {
        List<ResultadoRegla> resultados = whenEvaluar(ventasConElResto(1, 1), alfajor(), galletitas());

        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("name", "Combo Alfajor Jorgito + Galletitas Criollitas 3 U");
        valores.put("quantity", new BigDecimal("25"));
        valores.put("price", new BigDecimal("1850"));
        valores.put("cost", new BigDecimal("1400"));
        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.acciones()).containsExactly(Map.of(
                        "tipo", "CREAR_REGISTRO",
                        "texto", "Crear el combo",
                        "proyectoId", AvisoTestData.PROYECTO_ID.toString(),
                        "bloqueId", AvisoTestData.INVENTARIO_ID.toString(),
                        "valores", valores)));
    }

    @Test
    @DisplayName("El combo nace con las unidades que se pueden armar: el menor stock de los dos productos")
    void evaluarShouldCreateComboWithTheLowerStockOfBothProducts() {
        assertThat(valoresDeLaAccion(whenEvaluar(ventasConElResto(1, 1),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 800, 520),
                AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, 25, 1250, 880)))).containsEntry("quantity",
                new BigDecimal("25"));
        assertThat(valoresDeLaAccion(whenEvaluar(ventasConElResto(1, 1),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 12, 800, 520),
                AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, 25, 1250, 880)))).containsEntry("quantity",
                new BigDecimal("12"));
    }

    @Test
    @DisplayName("Si a un producto le falta el stock o está en negativo, el combo nace en cero y no en negativo")
    void evaluarShouldCreateComboWithoutStockWhenAProductHasNone() {
        assertThat(valoresDeLaAccion(whenEvaluar(ventasConElResto(1, 1),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, null, 800, 520), galletitas())))
                .containsEntry("quantity", BigDecimal.ZERO);
        assertThat(valoresDeLaAccion(whenEvaluar(ventasConElResto(1, 1), alfajor(),
                AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, null, 1250, 880))))
                .containsEntry("quantity", BigDecimal.ZERO);
        assertThat(valoresDeLaAccion(whenEvaluar(ventasConElResto(1, 1),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, -3, 800, 520), galletitas())))
                .containsEntry("quantity", BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Sin costos cargados el combo se crea sin costo")
    void evaluarShouldCreateComboWithoutCostWhenNoCostIsLoaded() {
        List<ResultadoRegla> resultados = whenEvaluar(ventasConElResto(1, 1),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 800, null),
                AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, 25, 1250, 0));

        assertThat(valoresDeLaAccion(resultados)).doesNotContainKey("cost");
    }

    @Test
    @DisplayName("Con el costo de uno solo, el combo cuesta lo que se cargó")
    void evaluarShouldUseTheOnlyLoadedCost() {
        List<ResultadoRegla> resultados = whenEvaluar(ventasConElResto(1, 1),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 800, 520),
                AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, 25, 1250, null));

        assertThat(valoresDeLaAccion(resultados)).containsEntry("cost", new BigDecimal("520"));
    }

    @Test
    @DisplayName("Sólo con el costo del segundo producto, el combo cuesta lo que se cargó")
    void evaluarShouldUseTheOnlyLoadedCostOfTheSecondProduct() {
        List<ResultadoRegla> resultados = whenEvaluar(ventasConElResto(1, 1),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 800, null),
                AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, 25, 1250, 880));

        assertThat(valoresDeLaAccion(resultados)).containsEntry("cost", new BigDecimal("880"));
    }

    @Test
    @DisplayName("Nombra los productos en el orden del inventario y guarda los precios en el orden del par")
    void evaluarShouldNameProductsInInventoryOrderAndKeepPricesInPairOrder() {
        List<ResultadoRegla> enOrden = whenEvaluar(ventasConElResto(1, 1), alfajor(), galletitas());
        List<ResultadoRegla> alReves = whenEvaluar(ventasConElResto(1, 1), galletitas(), alfajor());

        assertThat(enOrden.get(0).titulo()).startsWith("Alfajor Jorgito y ");
        assertThat(alReves.get(0).titulo()).startsWith("Galletitas Criollitas 3 U y ");
        assertThat(alReves.get(0).datos()).containsEntry("precioA", new BigDecimal("800"))
                .containsEntry("precioB", new BigDecimal("1250")).containsEntry("sugerido", new BigDecimal("1850"));
        assertThat(enOrden.get(0).datos()).isEqualTo(alReves.get(0).datos());
    }

    @Test
    @DisplayName("Avisa si sólo el primer producto del par rota poco")
    void evaluarShouldSuggestComboWhenOnlyTheFirstProductSellsLittle() {
        assertThat(whenEvaluar(ventasConElResto(1, 5), alfajor(), galletitas())).hasSize(1);
    }

    @Test
    @DisplayName("Avisa si sólo el segundo producto del par rota poco")
    void evaluarShouldSuggestComboWhenOnlyTheSecondProductSellsLittle() {
        assertThat(whenEvaluar(ventasConElResto(5, 1), alfajor(), galletitas())).hasSize(1);
    }

    @Test
    @DisplayName("No avisa si los dos productos se venden bien")
    void evaluarShouldReturnNothingWhenBothProductsSellWell() {
        assertThat(whenEvaluar(ventasConElResto(5, 5), alfajor(), galletitas())).isEmpty();
    }

    @Test
    @DisplayName("No avisa si los productos no se llevaron juntos las veces suficientes")
    void evaluarShouldReturnNothingWhenThereAreNoFrequentPairs() {
        List<Registro> ventas = ventasDelResto();
        ventas.addAll(canastas(DIAS.subList(0, 2), 1, 1));

        assertThat(whenEvaluar(ventas, alfajor(), galletitas())).isEmpty();
    }

    @Test
    @DisplayName("No avisa si el negocio ya tiene el combo de esos dos productos")
    void evaluarShouldReturnNothingWhenComboAlreadyExists() {
        Registro combo = AvisoTestData.producto(UUID.randomUUID(), "Combo alfajor + galletitas", 0, 1850, null);

        assertThat(whenEvaluar(ventasConElResto(1, 1), alfajor(), galletitas(), combo)).isEmpty();
    }

    @Test
    @DisplayName("Reconoce el combo existente aunque el nombre tenga tildes, mayúsculas y palabras vacías")
    void evaluarShouldRecognizeExistingComboIgnoringAccentsAndEmptyWords() {
        Registro combo = AvisoTestData.producto(UUID.randomUUID(), "COMBO Alfájor y Galletitas", 0, 1850, null);

        List<ResultadoRegla> resultados = whenEvaluar(ventasConElResto(1, 1),
                AvisoTestData.producto(ALFAJOR_ID, "Con Alfajor", 40, 800, 520),
                AvisoTestData.producto(GALLETITAS_ID, "500 Galletitas", 25, 1250, 880), combo);

        assertThat(resultados).isEmpty();
    }

    @Test
    @DisplayName("Un producto con otro nombre que contiene combo no cuenta como el combo de estos dos")
    void evaluarShouldNotTreatAnotherComboAsExisting() {
        Registro otroCombo = AvisoTestData.producto(UUID.randomUUID(), "Combo Yerba + Azúcar", 0, 1850, null);

        assertThat(whenEvaluar(ventasConElResto(1, 1), alfajor(), galletitas(), otroCombo)).hasSize(1);
    }

    @Test
    @DisplayName("Un combo que nombra sólo a uno de los dos productos no cuenta como el combo de estos dos")
    void evaluarShouldNotTreatComboOfOnlyOneProductAsExisting() {
        Registro otroCombo = AvisoTestData.producto(UUID.randomUUID(), "Combo Alfajor + Yerba", 0, 1850, null);

        assertThat(whenEvaluar(ventasConElResto(1, 1), alfajor(), galletitas(), otroCombo)).hasSize(1);
    }

    @Test
    @DisplayName("Si el primer producto no tiene una palabra significativa no se puede reconocer el combo y avisa")
    void evaluarShouldSuggestWhenFirstProductNameHasNoSignificantWord() {
        Registro combo = AvisoTestData.producto(UUID.randomUUID(), "Combo cualquiera", 0, 1850, null);

        List<ResultadoRegla> resultados = whenEvaluar(ventasConElResto(1, 1),
                AvisoTestData.producto(ALFAJOR_ID, "X", 40, 800, 520), galletitas(), combo);

        assertThat(resultados).hasSize(1);
    }

    @Test
    @DisplayName("Si el segundo producto no tiene una palabra significativa no se puede reconocer el combo y avisa")
    void evaluarShouldSuggestWhenSecondProductNameHasNoSignificantWord() {
        Registro combo = AvisoTestData.producto(UUID.randomUUID(), "Combo cualquiera", 0, 1850, null);

        List<ResultadoRegla> resultados = whenEvaluar(ventasConElResto(1, 1), alfajor(),
                AvisoTestData.producto(GALLETITAS_ID, "Y", 25, 1250, 880), combo);

        assertThat(resultados).hasSize(1);
    }

    @Test
    @DisplayName("No avisa si falta el precio del primer producto")
    void evaluarShouldReturnNothingWhenFirstProductHasNoPrice() {
        assertThat(whenEvaluar(ventasConElResto(1, 1),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, null, 520), galletitas())).isEmpty();
    }

    @Test
    @DisplayName("No avisa si falta el precio del segundo producto")
    void evaluarShouldReturnNothingWhenSecondProductHasNoPrice() {
        assertThat(whenEvaluar(ventasConElResto(1, 1), alfajor(),
                AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, 25, 0, 880))).isEmpty();
    }

    @Test
    @DisplayName("No avisa si el precio del combo redondeado queda en cero")
    void evaluarShouldReturnNothingWhenComboPriceRoundsToZero() {
        assertThat(whenEvaluar(ventasConElResto(1, 1),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 1, null),
                AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, 25, 1, null))).isEmpty();
    }

    @Test
    @DisplayName("No avisa si el precio del combo redondeado no queda por debajo de la suma")
    void evaluarShouldReturnNothingWhenComboPriceIsNotLowerThanTheSum() {
        // 8 x 0,9 = 7,2 se redondea a 10, que no es un descuento
        assertThat(whenEvaluar(ventasConElResto(1, 1),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 4, null),
                AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, 25, 4, null))).isEmpty();
    }

    @Test
    @DisplayName("No avisa si alguno de los dos productos ya no está en el inventario")
    void evaluarShouldReturnNothingWhenAProductIsNotInTheInventory() {
        assertThat(whenEvaluar(ventasConElResto(1, 1), alfajor())).isEmpty();
    }

    @Test
    @DisplayName("No avisa si los dos productos están en tablas de inventario distintas")
    void evaluarShouldReturnNothingWhenProductsAreInDifferentTables() {
        ContextoNegocio contexto = new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY,
                List.of(AvisoTestData.inventarioConPrecios(alfajor()), AvisoTestData.inventarioConPrecios(galletitas())),
                List.of(AvisoTestData.ventas(ventasConElResto(1, 1).toArray(new Registro[0]))), AnalisisZona.sinZona());

        assertThat(regla.evaluar(contexto)).isEmpty();
    }

    @Test
    @DisplayName("Busca el par en la tabla de inventario que tiene a los dos productos")
    void evaluarShouldFindTheTableThatHasBothProducts() {
        ContextoNegocio contexto = new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY,
                List.of(AvisoTestData.inventarioConPrecios(alfajor()),
                        AvisoTestData.inventarioConPrecios(alfajor(), galletitas())),
                List.of(AvisoTestData.ventas(ventasConElResto(1, 1).toArray(new Registro[0]))), AnalisisZona.sinZona());

        assertThat(regla.evaluar(contexto)).hasSize(1);
    }

    @Test
    @DisplayName("No avisa nada sin la fecha de hoy")
    void evaluarShouldReturnNothingWhenHoyIsMissing() {
        ContextoNegocio contexto = new ContextoNegocio(AvisoTestData.NEGOCIO_ID,
                List.of(AvisoTestData.inventarioConPrecios(alfajor(), galletitas())));

        assertThat(regla.evaluar(contexto)).isEmpty();
    }

    @Test
    @DisplayName("La huella no cambia si sólo cambian las veces que se vendieron juntos")
    void evaluarShouldKeepHuellaWhenOnlyTheTimesChange() {
        ResultadoRegla cuatroVeces = whenEvaluar(ventasConElResto(1, 1), alfajor(), galletitas()).get(0);
        List<Registro> cincoVeces = ventasConElResto(1, 1);
        cincoVeces.addAll(canastas(List.of("2026-10-18"), 1, 1));
        ResultadoRegla masVeces = whenEvaluar(cincoVeces, alfajor(), galletitas()).get(0);

        assertThat(masVeces.detalle()).contains("5 veces");
        assertThat(masVeces.huellaPara(TipoAviso.COMBO)).isEqualTo(cuatroVeces.huellaPara(TipoAviso.COMBO));
    }

    @Test
    @DisplayName("La huella cambia cuando cambia el precio de un producto")
    void evaluarShouldChangeHuellaWhenAPriceChanges() {
        ResultadoRegla antes = whenEvaluar(ventasConElResto(1, 1), alfajor(), galletitas()).get(0);
        ResultadoRegla despues = whenEvaluar(ventasConElResto(1, 1),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 900, 520), galletitas()).get(0);

        assertThat(despues.huellaPara(TipoAviso.COMBO)).isNotEqualTo(antes.huellaPara(TipoAviso.COMBO));
    }

    // --- act ---
    private List<ResultadoRegla> whenEvaluar(List<Registro> ventas, Registro... productos) {
        BloqueInventario inventario = AvisoTestData.inventarioConPrecios(productos);
        return regla.evaluar(new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY, List.of(inventario),
                List.of(AvisoTestData.ventas(ventas.toArray(new Registro[0]))), AnalisisZona.sinZona()));
    }

    // --- helpers ---
    @SuppressWarnings("unchecked")
    private static Map<String, Object> valoresDeLaAccion(List<ResultadoRegla> resultados) {
        Map<String, Object> accion = (Map<String, Object>) resultados.get(0).acciones().get(0);
        return (Map<String, Object>) accion.get("valores");
    }

    private static Registro alfajor() {
        return AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 800, 520);
    }

    private static Registro galletitas() {
        return AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, 25, 1250, 880);
    }

    /**
     * Cuatro canastas del mismo cliente con los dos productos (en las cantidades dadas) más las ventas de la yerba (20
     * unidades) y la gaseosa (15): la mediana de los demás es 15 o más, así que 4 unidades rotan poco y 20 no.
     */
    private static List<Registro> ventasConElResto(int cantidadAlfajor, int cantidadGalletitas) {
        List<Registro> ventas = ventasDelResto();
        ventas.addAll(canastas(DIAS, cantidadAlfajor, cantidadGalletitas));
        return ventas;
    }

    private static List<Registro> ventasDelResto() {
        List<Registro> ventas = new ArrayList<>();
        ventas.add(AvisoTestData.venta(YERBA_ID.toString(), 20, 3900, "2026-10-12", null));
        ventas.add(AvisoTestData.venta(COCA_ID.toString(), 15, 2750, "2026-10-14", null));
        return ventas;
    }

    private static List<Registro> canastas(List<String> dias, int cantidadAlfajor, int cantidadGalletitas) {
        List<Registro> ventas = new ArrayList<>();
        for (String dia : dias) {
            ventas.add(AvisoTestData.venta(ALFAJOR_ID.toString(), cantidadAlfajor, 800, dia, "Doña Rosa"));
            ventas.add(AvisoTestData.venta(GALLETITAS_ID.toString(), cantidadGalletitas, 1250, dia, "Doña Rosa"));
        }
        return ventas;
    }
}
