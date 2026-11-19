package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.AccionesDePrueba;
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
    private static final int POSICION_DEL_ALFAJOR = 0;
    private static final int POSICION_DE_LAS_GALLETITAS = 1;

    private ReglaComboBajaRotacion regla = unaReglaConDescuento(new BigDecimal("0.1"));
    private List<Registro> ventas = new ArrayList<>();
    private final List<Registro> productos = new ArrayList<>();

    private TipoAviso tipo;
    private List<ResultadoRegla> resultados;
    private List<ResultadoRegla> resultadosDelEscenarioBase;

    @Test
    @DisplayName("Es la regla de los avisos de combo")
    void esLaReglaDeLosAvisosDeCombo() {
        cuandoSePideElTipo();

        entoncesElTipoEsCombo();
    }

    @Test
    @DisplayName("Propone el combo de dos productos de baja rotación que se llevaron juntos, con un 10 % menos")
    void proponeElComboDeDosProductosDeBajaRotacionQueSeLlevaronJuntosConUnDiezPorCientoMenos() {
        dadoCuatroCanastasDeAlfajorYGalletitas();

        cuandoSeEvaluaElNegocio();

        entoncesSeProponeElComboDeLosDosProductos();
        entoncesSeExplicaElPrecioSugerido();
    }

    @Test
    @DisplayName("Ofrece crear el combo como un producto nuevo del inventario, con la suma de los costos")
    void ofreceCrearElComboComoUnProductoNuevoDelInventarioConLaSumaDeLosCostos() {
        dadoCuatroCanastasDeAlfajorYGalletitas();

        cuandoSeEvaluaElNegocio();

        entoncesSeOfreceCrearElComboConLaSumaDeLosCostos();
    }

    @Test
    @DisplayName("El combo no lleva costo si el alfajor no lo tiene")
    void elComboNoLlevaCostoSiElAlfajorNoLoTiene() {
        dadoCuatroCanastasDeAlfajorYGalletitas();
        dadoQueElAlfajorNoTieneCosto();

        cuandoSeEvaluaElNegocio();

        entoncesElComboNoLlevaCosto();
    }

    @Test
    @DisplayName("El combo no lleva costo si las galletitas lo tienen en cero")
    void elComboNoLlevaCostoSiLasGalletitasLoTienenEnCero() {
        dadoCuatroCanastasDeAlfajorYGalletitas();
        dadoQueLasGalletitasTienenCostoCero();

        cuandoSeEvaluaElNegocio();

        entoncesElComboNoLlevaCosto();
    }

    @Test
    @DisplayName("Si sólo el alfajor rota poco, lo dice")
    void siSoloElAlfajorRotaPocoLoDice() {
        dadoCuatroCanastasDeAlfajorYGalletitas();
        dadoQueLasGalletitasTambienSeVendenPorSeparado();

        cuandoSeEvaluaElNegocio();

        entoncesElDetalleEmpiezaCon("Alfajor Capitán del Espacio sale poco por separado, pero 4 veces");
    }

    @Test
    @DisplayName("Si sólo las galletitas rotan poco, lo dice")
    void siSoloLasGalletitasRotanPocoLoDice() {
        dadoCuatroCanastasDeAlfajorYGalletitas();
        dadoQueElAlfajorTambienSeVendePorSeparado();

        cuandoSeEvaluaElNegocio();

        entoncesElDetalleEmpiezaCon("Galletitas Sonrisas sale poco por separado, pero 4 veces");
    }

    @Test
    @DisplayName("No propone el combo si ninguno de los dos rota poco")
    void noProponeElComboSiNingunoDeLosDosRotaPoco() {
        dadoCuatroCanastasDeAlfajorYGalletitas();
        dadoQueAmbosSeVendenTambienPorSeparado();

        cuandoSeEvaluaElNegocio();

        entoncesNoSeProponeNingunCombo();
    }

    @Test
    @DisplayName("No propone el combo si el par se llevó juntos menos veces que la frecuencia mínima")
    void noProponeElComboSiElParSeLlevoJuntosMenosVecesQueLaFrecuenciaMinima() {
        dadoSoloDosCanastasDeAlfajorYGalletitas();

        cuandoSeEvaluaElNegocio();

        entoncesNoSeProponeNingunCombo();
    }

    @Test
    @DisplayName("Una canasta es el mismo cliente el mismo día: en otro día o con otro cliente no cuentan juntos")
    void unaCanastaEsElMismoClienteElMismoDiaEnOtroDiaOConOtroClienteNoCuentanJuntos() {
        dadoVentasDelParEnOtroDiaOConOtroCliente();

        cuandoSeEvaluaElNegocio();

        entoncesNoSeProponeNingunCombo();
    }

    @Test
    @DisplayName("Reconoce al mismo cliente aunque cambien las tildes y las mayúsculas")
    void reconoceAlMismoClienteAunqueCambienLasTildesYLasMayusculas() {
        dadoUnaCanastaConElClienteEscritoDeDosFormas();

        cuandoSeEvaluaElNegocio();

        entoncesSeProponeUnSoloCombo();
    }

    @Test
    @DisplayName("Sin cliente en las ventas no hay canastas y no avisa")
    void sinClienteEnLasVentasNoHayCanastasYNoAvisa() {
        dadoVentasDelParSinCliente();

        cuandoSeEvaluaElNegocio();

        entoncesNoSeProponeNingunCombo();
    }

    @Test
    @DisplayName("Las canastas de hace más de un período no cuentan")
    void lasCanastasDeHaceMasDeUnPeriodoNoCuentan() {
        dadoCanastasDeHaceMasDeUnPeriodo();

        cuandoSeEvaluaElNegocio();

        entoncesNoSeProponeNingunCombo();
    }

    @Test
    @DisplayName("No propone el combo si ya existe uno en el inventario que nombra a los dos productos")
    void noProponeElComboSiYaExisteUnoEnElInventarioQueNombraALosDosProductos() {
        dadoCuatroCanastasDeAlfajorYGalletitas();
        dadoUnComboYaCreadoConLosDosProductos();

        cuandoSeEvaluaElNegocio();

        entoncesNoSeProponeNingunCombo();
    }

    @Test
    @DisplayName("Otro combo o un producto que nombra a uno solo no impide proponerlo")
    void otroComboOUnProductoQueNombraAUnoSoloNoImpideProponerlo() {
        dadoCuatroCanastasDeAlfajorYGalletitas();
        dadoOtroComboYUnProductoQueNombraAUnoSolo();

        cuandoSeEvaluaElNegocio();

        entoncesSeProponeUnSoloCombo();
    }

    @Test
    @DisplayName("No propone el combo si el alfajor no tiene precio")
    void noProponeElComboSiElAlfajorNoTienePrecio() {
        dadoCuatroCanastasDeAlfajorYGalletitas();
        dadoQueElAlfajorNoTienePrecio();

        cuandoSeEvaluaElNegocio();

        entoncesNoSeProponeNingunCombo();
    }

    @Test
    @DisplayName("No propone el combo si las galletitas tienen el precio en cero")
    void noProponeElComboSiLasGalletitasTienenElPrecioEnCero() {
        dadoCuatroCanastasDeAlfajorYGalletitas();
        dadoQueLasGalletitasTienenPrecioCero();

        cuandoSeEvaluaElNegocio();

        entoncesNoSeProponeNingunCombo();
    }

    @Test
    @DisplayName("No propone el combo si el alfajor tiene el precio en cero")
    void noProponeElComboSiElAlfajorTieneElPrecioEnCero() {
        dadoCuatroCanastasDeAlfajorYGalletitas();
        dadoQueElAlfajorTienePrecioCero();

        cuandoSeEvaluaElNegocio();

        entoncesNoSeProponeNingunCombo();
    }

    @Test
    @DisplayName("No propone el combo si el precio sugerido no queda por debajo de la suma")
    void noProponeElComboSiElPrecioSugeridoNoQuedaPorDebajoDeLaSuma() {
        dadoCuatroCanastasDeAlfajorYGalletitas();
        dadoUnaReglaSinDescuento();

        cuandoSeEvaluaElNegocio();

        entoncesNoSeProponeNingunCombo();
    }

    @Test
    @DisplayName("No propone el combo si alguno de los dos productos ya no está en el inventario")
    void noProponeElComboSiAlgunoDeLosDosProductosYaNoEstaEnElInventario() {
        dadoCuatroCanastasDeAlfajorYGalletitas();
        dadoQueLasGalletitasYaNoEstanEnElInventario();

        cuandoSeEvaluaElNegocio();

        entoncesNoSeProponeNingunCombo();
    }

    @Test
    @DisplayName("No avisa sin la fecha de hoy")
    void noAvisaSinLaFechaDeHoy() {
        dadoCuatroCanastasDeAlfajorYGalletitas();

        cuandoSeEvaluaElNegocioSinLaFechaDeHoy();

        entoncesNoSeProponeNingunCombo();
    }

    @Test
    @DisplayName("No avisa sin ventas")
    void noAvisaSinVentas() {
        dadoUnInventarioConAlfajorYGalletitasSinVentas();

        cuandoSeEvaluaElNegocio();

        entoncesNoSeProponeNingunCombo();
    }

    @Test
    @DisplayName("La huella no cambia con la cantidad de canastas")
    void laHuellaNoCambiaConLaCantidadDeCanastas() {
        dadoCuatroCanastasDeAlfajorYGalletitas();
        dadoUnaCanastaMas();

        cuandoSeEvaluaElNegocioYElEscenarioBase();

        entoncesLaHuellaEsLaMismaQueLaDelEscenarioBase();
    }

    @Test
    @DisplayName("La huella cambia cuando cambia un precio")
    void laHuellaCambiaCuandoCambiaUnPrecio() {
        dadoCuatroCanastasDeAlfajorYGalletitas();
        dadoQueElAlfajorCuestaNovecientos();

        cuandoSeEvaluaElNegocioYElEscenarioBase();

        entoncesLaHuellaDifiereDeLaDelEscenarioBase();
    }

    // --- dado ---

    private void dadoCuatroCanastasDeAlfajorYGalletitas() {
        ventas = ventasConCuatroCanastas();
        dadoElInventarioDeAlfajorYGalletitas();
    }

    private void dadoUnInventarioConAlfajorYGalletitasSinVentas() {
        ventas = new ArrayList<>();
        dadoElInventarioDeAlfajorYGalletitas();
    }

    private void dadoElInventarioDeAlfajorYGalletitas() {
        productos.clear();
        productos.addAll(List.of(alfajor(), galletitas()));
    }

    private void dadoQueElAlfajorNoTieneCosto() {
        productos.set(POSICION_DEL_ALFAJOR, AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 800, null));
    }

    private void dadoQueLasGalletitasTienenCostoCero() {
        productos.set(POSICION_DE_LAS_GALLETITAS, AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, 25, 1100, 0));
    }

    private void dadoQueLasGalletitasTambienSeVendenPorSeparado() {
        ventas.add(venta(GALLETITAS_ID, 40, 1100, "2026-10-14", null));
    }

    private void dadoQueElAlfajorTambienSeVendePorSeparado() {
        ventas.add(venta(ALFAJOR_ID, 40, 800, "2026-10-14", null));
    }

    private void dadoQueAmbosSeVendenTambienPorSeparado() {
        dadoQueElAlfajorTambienSeVendePorSeparado();
        dadoQueLasGalletitasTambienSeVendenPorSeparado();
    }

    private void dadoSoloDosCanastasDeAlfajorYGalletitas() {
        ventas = ventasDelResto();
        ventas.addAll(canasta("2026-10-16", "Doña Rosa"));
        ventas.addAll(canasta("2026-10-10", "Bar La Esquina"));
        dadoElInventarioDeAlfajorYGalletitas();
    }

    private void dadoVentasDelParEnOtroDiaOConOtroCliente() {
        ventas = ventasDelResto();
        for (String dia : List.of("2026-10-16", "2026-10-10", "2026-10-04", "2026-09-27")) {
            ventas.add(venta(ALFAJOR_ID, 1, 800, dia, "Doña Rosa"));
            ventas.add(venta(GALLETITAS_ID, 1, 1100, dia, "Bar La Esquina"));
        }
        ventas.add(venta(ALFAJOR_ID, 1, 800, "2026-10-17", "Doña Rosa"));
        ventas.add(venta(GALLETITAS_ID, 1, 1100, "2026-10-18", "Doña Rosa"));
        dadoElInventarioDeAlfajorYGalletitas();
    }

    private void dadoUnaCanastaConElClienteEscritoDeDosFormas() {
        ventas = ventasDelResto();
        ventas.add(venta(ALFAJOR_ID, 1, 800, "2026-10-16", "Doña Rosa"));
        ventas.add(venta(GALLETITAS_ID, 1, 1100, "2026-10-16", "DOÑA ROSA"));
        ventas.addAll(canasta("2026-10-10", "Bar La Esquina"));
        ventas.addAll(canasta("2026-10-04", "Club Social"));
        dadoElInventarioDeAlfajorYGalletitas();
    }

    private void dadoVentasDelParSinCliente() {
        ventas = ventasDelResto();
        for (String dia : List.of("2026-10-16", "2026-10-10", "2026-10-04", "2026-09-27")) {
            ventas.add(venta(ALFAJOR_ID, 1, 800, dia, null));
            ventas.add(venta(GALLETITAS_ID, 1, 1100, dia, null));
        }
        dadoElInventarioDeAlfajorYGalletitas();
    }

    private void dadoCanastasDeHaceMasDeUnPeriodo() {
        ventas = ventasDelResto();
        for (String dia : List.of("2026-08-16", "2026-08-10", "2026-08-04", "2026-07-27")) {
            ventas.addAll(canasta(dia, "Doña Rosa"));
        }
        dadoElInventarioDeAlfajorYGalletitas();
    }

    private void dadoUnComboYaCreadoConLosDosProductos() {
        productos.add(AvisoTestData.producto(UUID.randomUUID(),
                "Combo Alfajor Capitán del Espacio + Galletitas Sonrisas", 0, 1710, null));
    }

    private void dadoOtroComboYUnProductoQueNombraAUnoSolo() {
        productos.add(AvisoTestData.producto(UUID.randomUUID(), "Combo Yerba + Fideos", 0, 3000, null));
        productos.add(AvisoTestData.producto(UUID.randomUUID(), "Combo Alfajor Capitán del Espacio x3", 0, 2000, null));
    }

    private void dadoQueElAlfajorNoTienePrecio() {
        productos.set(POSICION_DEL_ALFAJOR, AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, null, 520));
    }

    private void dadoQueLasGalletitasTienenPrecioCero() {
        productos.set(POSICION_DE_LAS_GALLETITAS, AvisoTestData.producto(GALLETITAS_ID, GALLETITAS, 25, 0, 750));
    }

    private void dadoQueElAlfajorTienePrecioCero() {
        productos.set(POSICION_DEL_ALFAJOR, AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 0, 520));
    }

    private void dadoUnaReglaSinDescuento() {
        regla = unaReglaConDescuento(BigDecimal.ZERO);
    }

    private void dadoQueLasGalletitasYaNoEstanEnElInventario() {
        productos.remove(POSICION_DE_LAS_GALLETITAS);
    }

    private void dadoUnaCanastaMas() {
        ventas.addAll(canasta("2026-10-12", "Doña Rosa"));
    }

    private void dadoQueElAlfajorCuestaNovecientos() {
        productos.set(POSICION_DEL_ALFAJOR, AvisoTestData.producto(ALFAJOR_ID, ALFAJOR, 40, 900, 520));
    }

    // --- cuando ---

    private void cuandoSePideElTipo() {
        tipo = regla.tipo();
    }

    private void cuandoSeEvaluaElNegocio() {
        resultados = regla.evaluar(contextoDeHoy());
    }

    private void cuandoSeEvaluaElNegocioSinLaFechaDeHoy() {
        ContextoNegocio sinHoy = new ContextoNegocio(AvisoTestData.NEGOCIO_ID, List.of(inventarioActual()));
        resultados = regla.evaluar(sinHoy);
    }

    private void cuandoSeEvaluaElNegocioYElEscenarioBase() {
        cuandoSeEvaluaElNegocio();
        resultadosDelEscenarioBase = evaluarElEscenarioBase();
    }

    // --- entonces ---

    private void entoncesElTipoEsCombo() {
        assertThat(tipo).isEqualTo(TipoAviso.COMBO);
    }

    private void entoncesSeProponeElComboDeLosDosProductos() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.entidad())
                    .isEqualTo(ResultadoRegla.entidadDeCombo(RegistroId.de(ALFAJOR_ID), RegistroId.de(GALLETITAS_ID)));
            assertThat(resultado.prioridad()).isEqualTo((short) 1);
            assertThat(resultado.proyectoId()).isEqualTo(AvisoTestData.PROYECTO_ID);
            assertThat(resultado.titulo()).isEqualTo("Alfajor Capitán del Espacio y Galletitas Sonrisas se venden juntos");
            assertThat(resultado.detalle()).isEqualTo("Los dos salen poco por separado, pero 4 veces se los llevó "
                    + "el mismo cliente el mismo día.");
        });
    }

    private void entoncesSeExplicaElPrecioSugerido() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.motivos()).containsExactly(
                    "Por separado suman $1.900; como combo, $1.710 (10 % menos) puede ayudar a que roten.",
                    "El combo se crea como un producto nuevo del inventario; después cargás cuántos armás.");
            assertThat(resultado.datos()).containsEntry("precioA", new BigDecimal("800"))
                    .containsEntry("precioB", new BigDecimal("1100")).containsEntry("sugerido", new BigDecimal("1710"));
        });
    }

    private void entoncesSeOfreceCrearElComboConLaSumaDeLosCostos() {
        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.acciones()).singleElement().isEqualTo(AccionesDePrueba.crearEnElInventario(
                        "Crear el combo", Map.of(
                                "name", "Combo Alfajor Capitán del Espacio + Galletitas Sonrisas",
                                "quantity", 0,
                                "price", new BigDecimal("1710"),
                                "cost", new BigDecimal("1270")))));
    }

    private void entoncesElComboNoLlevaCosto() {
        assertThat(valoresDeLaAccion(resultados.get(0))).doesNotContainKey("cost");
    }

    private void entoncesElDetalleEmpiezaCon(String texto) {
        assertThat(resultados).singleElement().satisfies(resultado -> assertThat(resultado.detalle()).startsWith(texto));
    }

    private void entoncesNoSeProponeNingunCombo() {
        assertThat(resultados).isEmpty();
    }

    private void entoncesSeProponeUnSoloCombo() {
        assertThat(resultados).hasSize(1);
    }

    private void entoncesLaHuellaEsLaMismaQueLaDelEscenarioBase() {
        assertThat(huellaDe(resultados)).isEqualTo(huellaDe(resultadosDelEscenarioBase));
    }

    private void entoncesLaHuellaDifiereDeLaDelEscenarioBase() {
        assertThat(huellaDe(resultados)).isNotEqualTo(huellaDe(resultadosDelEscenarioBase));
    }

    // --- datos ---

    private static ReglaComboBajaRotacion unaReglaConDescuento(BigDecimal descuento) {
        return new ReglaComboBajaRotacion(new MetricasVentas(30, new BigDecimal("0.5")), 3, descuento);
    }

    private ContextoNegocio contextoDeHoy() {
        return new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY, List.of(inventarioActual()),
                List.of(AvisoTestData.ventas(ventas.toArray(new Registro[0]))), AnalisisZona.sinZona());
    }

    private BloqueInventario inventarioActual() {
        return AvisoTestData.inventarioConPrecios(productos.toArray(new Registro[0]));
    }

    private List<ResultadoRegla> evaluarElEscenarioBase() {
        BloqueInventario inventario = AvisoTestData.inventarioConPrecios(alfajor(), galletitas());
        return regla.evaluar(new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY, List.of(inventario),
                List.of(AvisoTestData.ventas(ventasConCuatroCanastas().toArray(new Registro[0]))),
                AnalisisZona.sinZona()));
    }

    private static String huellaDe(List<ResultadoRegla> resultados) {
        return resultados.get(0).huellaPara(TipoAviso.COMBO);
    }

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

    private static Map<String, Object> valoresDeLaAccion(ResultadoRegla resultado) {
        return ((AccionCrearRegistro) resultado.acciones().get(0)).valores();
    }
}
