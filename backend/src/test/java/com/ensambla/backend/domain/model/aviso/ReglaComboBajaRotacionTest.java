package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
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

class ReglaComboBajaRotacionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final UUID ALFAJOR_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID GALLETITAS_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID YERBA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000003");
    private static final UUID LECHE_ID = UUID.fromString("d0000000-0000-4000-8000-000000000004");
    private static final String ALFAJOR = "Alfajor Capitán del Espacio";
    private static final String GALLETITAS = "Galletitas Sonrisas";

    private final ReglaComboBajaRotacion regla = new ReglaComboBajaRotacion(
            new MetricasVentas(30, new BigDecimal("0.5")), 3, new BigDecimal("0.1"));

    @Test
    @DisplayName("Es la regla de los avisos de combo")
    void tipoShouldBeCombo() {
        assertThat(regla.tipo()).isEqualTo(TipoAviso.COMBO);
    }

    @Test
    @DisplayName("Propone el combo de dos productos de baja rotación que se llevaron juntos, con un 10 % menos")
    void evaluarShouldProposeComboWhenPairSellsTogetherAndRotatesLittle() {
        List<ResultadoRegla> resultados = whenEvaluar(ventasConCuatroCanastas(), alfajor(), galletitas());

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.entidad())
                    .isEqualTo(ResultadoRegla.entidadDeCombo(RegistroId.de(ALFAJOR_ID), RegistroId.de(GALLETITAS_ID)));
            assertThat(resultado.prioridad()).isEqualTo((short) 1);
            assertThat(resultado.proyectoId()).isEqualTo(AvisoTestData.PROYECTO_ID);
            assertThat(resultado.titulo()).isEqualTo("Alfajor Capitán del Espacio y Galletitas Sonrisas se venden juntos");
            assertThat(resultado.detalle()).isEqualTo("Los dos salen poco por separado, pero 4 veces se los llevó "
                    + "el mismo cliente el mismo día.");
            assertThat(resultado.motivos()).containsExactly(
                    "Por separado suman $1.900; como combo, $1.710 (10 % menos) puede ayudar a que roten.",
                    "El combo se crea como un producto nuevo del inventario; después cargás cuántos armás.");
            assertThat(resultado.datos()).containsEntry("precioA", new BigDecimal("800"))
                    .containsEntry("precioB", new BigDecimal("1100")).containsEntry("sugerido", new BigDecimal("1710"));
        });
    }

    @Test
    @DisplayName("Ofrece crear el combo como un producto nuevo del inventario, con la suma de los costos")
    void evaluarShouldOfferCrearRegistroAction() {
        List<ResultadoRegla> resultados = whenEvaluar(ventasConCuatroCanastas(), alfajor(), galletitas());

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.acciones()).singleElement().isEqualTo(Map.of(
                    "tipo", "CREAR_REGISTRO",
                    "texto", "Crear el combo",
                    "proyectoId", AvisoTestData.PROYECTO_ID.toString(),
                    "bloqueId", AvisoTestData.INVENTARIO_ID.toString(),
                    "valores", Map.of(
                            "name", "Combo Alfajor Capitán del Espacio + Galletitas Sonrisas",
                            "quantity", 0,
                            "price", new BigDecimal("1710"),
                            "cost", new BigDecimal("1270"))));
        });
    }

    @Test
    @DisplayName("El combo no lleva costo si alguno de los productos no lo tiene")
    void evaluarShouldOmitCostWhenAnyProductHasNoCost() {
        List<ResultadoRegla> sinCostoA = whenEvaluar(ventasConCuatroCanastas(),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 800, null), galletitas());
        List<ResultadoRegla> sinCostoB = whenEvaluar(ventasConCuatroCanastas(), alfajor(),
                AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, 25, 1100, 0));

        assertThat(valoresDeLaAccion(sinCostoA.get(0))).doesNotContainKey("cost");
        assertThat(valoresDeLaAccion(sinCostoB.get(0))).doesNotContainKey("cost");
    }

    @Test
    @DisplayName("Si sólo uno de los dos rota poco, dice cuál")
    void evaluarShouldNameTheOneThatRotatesLittle() {
        List<Registro> galletitasSeVenden = ventasConCuatroCanastas();
        galletitasSeVenden.add(venta(GALLETITAS_ID, 40, 1100, "2026-10-14", null));
        List<Registro> alfajorSeVende = ventasConCuatroCanastas();
        alfajorSeVende.add(venta(ALFAJOR_ID, 40, 800, "2026-10-14", null));

        List<ResultadoRegla> bajaElAlfajor = whenEvaluar(galletitasSeVenden, alfajor(), galletitas());
        List<ResultadoRegla> bajaLaGalletita = whenEvaluar(alfajorSeVende, alfajor(), galletitas());

        assertThat(bajaElAlfajor).singleElement().satisfies(resultado -> assertThat(resultado.detalle())
                .startsWith("Alfajor Capitán del Espacio sale poco por separado, pero 4 veces"));
        assertThat(bajaLaGalletita).singleElement().satisfies(resultado -> assertThat(resultado.detalle())
                .startsWith("Galletitas Sonrisas sale poco por separado, pero 4 veces"));
    }

    @Test
    @DisplayName("No propone el combo si ninguno de los dos rota poco")
    void evaluarShouldReturnNothingWhenNeitherRotatesLittle() {
        List<Registro> ventas = ventasConCuatroCanastas();
        ventas.add(venta(ALFAJOR_ID, 40, 800, "2026-10-14", null));
        ventas.add(venta(GALLETITAS_ID, 40, 1100, "2026-10-14", null));

        assertThat(whenEvaluar(ventas, alfajor(), galletitas())).isEmpty();
    }

    @Test
    @DisplayName("No propone el combo si el par se llevó juntos menos veces que la frecuencia mínima")
    void evaluarShouldReturnNothingWhenPairIsNotFrequentEnough() {
        List<Registro> ventas = ventasDelResto();
        ventas.addAll(canasta("2026-10-16", "Doña Rosa"));
        ventas.addAll(canasta("2026-10-10", "Bar La Esquina"));

        assertThat(whenEvaluar(ventas, alfajor(), galletitas())).isEmpty();
    }

    @Test
    @DisplayName("Una canasta es el mismo cliente el mismo día: en otro día o con otro cliente no cuentan juntos")
    void evaluarShouldNotCountSalesOfDifferentDaysOrClientsAsTheSameBasket() {
        List<Registro> ventas = ventasDelResto();
        for (String dia : List.of("2026-10-16", "2026-10-10", "2026-10-04", "2026-09-27")) {
            ventas.add(venta(ALFAJOR_ID, 1, 800, dia, "Doña Rosa"));
            ventas.add(venta(GALLETITAS_ID, 1, 1100, dia, "Bar La Esquina"));
        }
        ventas.add(venta(ALFAJOR_ID, 1, 800, "2026-10-17", "Doña Rosa"));
        ventas.add(venta(GALLETITAS_ID, 1, 1100, "2026-10-18", "Doña Rosa"));

        assertThat(whenEvaluar(ventas, alfajor(), galletitas())).isEmpty();
    }

    @Test
    @DisplayName("Reconoce al mismo cliente aunque cambien las tildes y las mayúsculas")
    void evaluarShouldMatchClientIgnoringAccentsAndCase() {
        List<Registro> ventas = ventasDelResto();
        ventas.add(venta(ALFAJOR_ID, 1, 800, "2026-10-16", "Doña Rosa"));
        ventas.add(venta(GALLETITAS_ID, 1, 1100, "2026-10-16", "DOÑA ROSA"));
        ventas.addAll(canasta("2026-10-10", "Bar La Esquina"));
        ventas.addAll(canasta("2026-10-04", "Club Social"));

        assertThat(whenEvaluar(ventas, alfajor(), galletitas())).hasSize(1);
    }

    @Test
    @DisplayName("Sin cliente en las ventas no hay canastas y no avisa")
    void evaluarShouldReturnNothingWhenSalesHaveNoClient() {
        List<Registro> ventas = ventasDelResto();
        for (String dia : List.of("2026-10-16", "2026-10-10", "2026-10-04", "2026-09-27")) {
            ventas.add(venta(ALFAJOR_ID, 1, 800, dia, null));
            ventas.add(venta(GALLETITAS_ID, 1, 1100, dia, null));
        }

        assertThat(whenEvaluar(ventas, alfajor(), galletitas())).isEmpty();
    }

    @Test
    @DisplayName("Las canastas de hace más de un período no cuentan")
    void evaluarShouldIgnoreBasketsOlderThanThePeriod() {
        List<Registro> ventas = ventasDelResto();
        for (String dia : List.of("2026-08-16", "2026-08-10", "2026-08-04", "2026-07-27")) {
            ventas.addAll(canasta(dia, "Doña Rosa"));
        }

        assertThat(whenEvaluar(ventas, alfajor(), galletitas())).isEmpty();
    }

    @Test
    @DisplayName("No propone el combo si ya existe uno en el inventario que nombra a los dos productos")
    void evaluarShouldReturnNothingWhenComboAlreadyExists() {
        Registro combo = AvisoTestData.producto(UUID.randomUUID(),
                "Combo Alfajor Capitán del Espacio + Galletitas Sonrisas", 0, 1710, null);

        assertThat(whenEvaluar(ventasConCuatroCanastas(), alfajor(), galletitas(), combo)).isEmpty();
    }

    @Test
    @DisplayName("Otro combo o un producto que nombra a uno solo no impide proponerlo")
    void evaluarShouldStillProposeWhenOtherProductsMentionOnlyOneOrNoneOfThem() {
        Registro otroCombo = AvisoTestData.producto(UUID.randomUUID(), "Combo Yerba + Fideos", 0, 3000, null);
        Registro soloUno = AvisoTestData.producto(UUID.randomUUID(), "Combo Alfajor Capitán del Espacio x3", 0, 2000,
                null);

        assertThat(whenEvaluar(ventasConCuatroCanastas(), alfajor(), galletitas(), otroCombo, soloUno)).hasSize(1);
    }

    @Test
    @DisplayName("No propone el combo si un producto no tiene precio o lo tiene en cero")
    void evaluarShouldReturnNothingWhenProductHasNoPrice() {
        assertThat(whenEvaluar(ventasConCuatroCanastas(),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, null, 520), galletitas())).isEmpty();
        assertThat(whenEvaluar(ventasConCuatroCanastas(), alfajor(),
                AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, 25, 0, 750))).isEmpty();
        assertThat(whenEvaluar(ventasConCuatroCanastas(),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 0, 520), galletitas())).isEmpty();
    }

    @Test
    @DisplayName("No propone el combo si el precio sugerido no queda por debajo de la suma")
    void evaluarShouldReturnNothingWhenComboPriceIsNotLowerThanTheSum() {
        ReglaComboBajaRotacion sinDescuento = new ReglaComboBajaRotacion(
                new MetricasVentas(30, new BigDecimal("0.5")), 3, BigDecimal.ZERO);

        assertThat(evaluar(sinDescuento, ventasConCuatroCanastas(), alfajor(), galletitas())).isEmpty();
    }

    @Test
    @DisplayName("No propone el combo si alguno de los dos productos ya no está en el inventario")
    void evaluarShouldReturnNothingWhenAProductIsNotInInventory() {
        assertThat(whenEvaluar(ventasConCuatroCanastas(), alfajor())).isEmpty();
    }

    @Test
    @DisplayName("No avisa sin la fecha de hoy ni sin ventas")
    void evaluarShouldReturnNothingWithoutHoyOrSales() {
        ContextoNegocio sinHoy = new ContextoNegocio(AvisoTestData.NEGOCIO_ID,
                List.of(AvisoTestData.inventarioConPrecios(alfajor(), galletitas())));

        assertThat(regla.evaluar(sinHoy)).isEmpty();
        assertThat(whenEvaluar(List.of(), alfajor(), galletitas())).isEmpty();
    }

    @Test
    @DisplayName("La huella no cambia con la cantidad de canastas pero sí cuando cambia un precio")
    void evaluarShouldKeepHuellaWhenOnlyBasketsChangeAndChangeItWhenPriceChanges() {
        ResultadoRegla base = whenEvaluar(ventasConCuatroCanastas(), alfajor(), galletitas()).get(0);
        List<Registro> masCanastas = ventasConCuatroCanastas();
        masCanastas.addAll(canasta("2026-10-12", "Doña Rosa"));
        ResultadoRegla conMasCanastas = whenEvaluar(masCanastas, alfajor(), galletitas()).get(0);
        ResultadoRegla otroPrecio = whenEvaluar(ventasConCuatroCanastas(),
                AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 900, 520), galletitas()).get(0);

        assertThat(conMasCanastas.huellaPara(TipoAviso.COMBO)).isEqualTo(base.huellaPara(TipoAviso.COMBO));
        assertThat(otroPrecio.huellaPara(TipoAviso.COMBO)).isNotEqualTo(base.huellaPara(TipoAviso.COMBO));
    }

    // --- act ---
    private List<ResultadoRegla> whenEvaluar(List<Registro> ventas, Registro... productos) {
        return evaluar(regla, ventas, productos);
    }

    private static List<ResultadoRegla> evaluar(ReglaComboBajaRotacion regla, List<Registro> ventas,
                                                Registro... productos) {
        BloqueInventario inventario = AvisoTestData.inventarioConPrecios(productos);
        return regla.evaluar(new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY, List.of(inventario),
                List.of(AvisoTestData.ventas(ventas.toArray(new Registro[0]))), AnalisisZona.sinZona()));
    }

    // --- helpers ---
    private static Registro alfajor() {
        return AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 800, 520);
    }

    private static Registro galletitas() {
        return AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, 25, 1100, 750);
    }

    /** La yerba vende 20 y la leche 30 en el período, sin cliente: la mediana de los demás es 20. */
    private static List<Registro> ventasDelResto() {
        List<Registro> ventas = new ArrayList<>();
        ventas.add(venta(YERBA_ID, 20, 3600, "2026-10-17", null));
        ventas.add(venta(LECHE_ID, 30, 1650, "2026-10-15", null));
        return ventas;
    }

    /** Cuatro canastas con el alfajor y las galletitas: 4 unidades de cada uno en el período. */
    private static List<Registro> ventasConCuatroCanastas() {
        List<Registro> ventas = ventasDelResto();
        ventas.addAll(canasta("2026-10-16", "Doña Rosa"));
        ventas.addAll(canasta("2026-10-10", "Bar La Esquina"));
        ventas.addAll(canasta("2026-10-04", "Club Social"));
        ventas.addAll(canasta("2026-09-27", "Doña Rosa"));
        return ventas;
    }

    private static List<Registro> canasta(String dia, String cliente) {
        return List.of(venta(ALFAJOR_ID, 1, 800, dia, cliente), venta(GALLETITAS_ID, 1, 1100, dia, cliente));
    }

    private static Registro venta(UUID producto, int cantidad, int precio, String fecha, String cliente) {
        return AvisoTestData.venta(producto.toString(), cantidad, precio, fecha, cliente);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> valoresDeLaAccion(ResultadoRegla resultado) {
        return (Map<String, Object>) ((Map<String, Object>) resultado.acciones().get(0)).get("valores");
    }
}
