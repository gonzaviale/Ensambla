package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.AccionesDePrueba;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReglaProductoAltaRotacionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final UUID YERBA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final String NOMBRE = "Yerba Nobleza Gaucha 1 kg";

    private final ReglaProductoAltaRotacion regla = new ReglaProductoAltaRotacion(
            new MetricasVentas(30, new BigDecimal("0.5")));

    private AnalisisZona zona = zona(3500, 4800, 8);
    private List<Registro> ventas = ventasEnAlza();
    private Registro yerba = yerba(3400, 2700);

    private TipoAviso tipo;
    private List<ResultadoRegla> resultados;
    private List<ResultadoRegla> resultadosDelEscenarioBase;

    @Test
    @DisplayName("Es la regla de los avisos de producto muy vendido")
    void esLaReglaDeLosAvisosDeProductoMuyVendido() {
        cuandoSePideElTipo();

        entoncesElTipoEsMuyVendido();
    }

    @Test
    @DisplayName("Avisa del producto que vende más que su promedio y está bajo el rango de la zona, con el aumento sugerido")
    void avisaDelProductoQueVendeMasQueSuPromedioYEstaBajoElRangoDeLaZonaConElAumentoSugerido() {
        dadoUnaYerbaQueVendeMasQueSuPromedioYEstaBajoElRangoDeLaZona();

        cuandoSeEvaluaElNegocio();

        entoncesAvisaQueLaYerbaSeVendeMasQueDeCostumbre();
        entoncesExplicaLasVentasElRangoYElMargen();
    }

    @Test
    @DisplayName("Ofrece subir el precio con una acción que edita el registro y otra que abre el producto")
    void ofreceSubirElPrecioConUnaAccionQueEditaElRegistroYOtraQueAbreElProducto() {
        dadoUnaYerbaQueVendeMasQueSuPromedioYEstaBajoElRangoDeLaZona();

        cuandoSeEvaluaElNegocio();

        entoncesOfreceSubirElPrecioYAbrirElProducto();
    }

    @Test
    @DisplayName("Sin costo cargado no habla del margen")
    void sinCostoCargadoNoHablaDelMargen() {
        dadoUnaYerbaDe(3400, null);

        cuandoSeEvaluaElNegocio();

        entoncesLosMotivosNoHablanDelMargen();
    }

    @Test
    @DisplayName("Con el costo en cero no habla del margen")
    void conElCostoEnCeroNoHablaDelMargen() {
        dadoUnaYerbaDe(3400, 0);

        cuandoSeEvaluaElNegocio();

        entoncesLosMotivosNoHablanDelMargen();
    }

    @Test
    @DisplayName("Si redondear el mínimo a la decena no lo deja por encima del precio actual, sugiere el mínimo exacto")
    void siRedondearElMinimoALaDecenaNoLoDejaPorEncimaDelPrecioActualSugiereElMinimoExacto() {
        dadoUnaZonaConMinimoDe(3494);
        dadoUnaYerbaDe(3492, null);

        cuandoSeEvaluaElNegocio();

        entoncesElPrecioSugeridoEs("3494");
    }

    @Test
    @DisplayName("No avisa si las ventas del período no superan su promedio")
    void noAvisaSiLasVentasDelPeriodoNoSuperanSuPromedio() {
        dadoSoloUnaVentaDeVeinteUnidades();

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa si el precio es el mínimo del rango de la zona")
    void noAvisaSiElPrecioEsElMinimoDelRangoDeLaZona() {
        dadoUnaYerbaDe(3500, 2700);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa si el precio ya está dentro del rango de la zona")
    void noAvisaSiElPrecioYaEstaDentroDelRangoDeLaZona() {
        dadoUnaYerbaDe(4000, 2700);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa si el producto no está en la zona")
    void noAvisaSiElProductoNoEstaEnLaZona() {
        dadoUnaZonaQueSoloConoceOtroProducto();

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa si la zona no alcanza el mínimo de comercios")
    void noAvisaSiLaZonaNoAlcanzaElMinimoDeComercios() {
        dadoUnaZonaDondeSoloDosComerciosVendenLaYerba();

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa sin zona")
    void noAvisaSinZona() {
        dadoUnNegocioSinZona();

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa de un producto sin ventas anotadas")
    void noAvisaDeUnProductoSinVentasAnotadas() {
        dadoUnNegocioSinVentas();

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa de un producto sin precio")
    void noAvisaDeUnProductoSinPrecio() {
        dadoUnaYerbaDe(null, 2700);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa de un producto con el precio en cero")
    void noAvisaDeUnProductoConElPrecioEnCero() {
        dadoUnaYerbaDe(0, 2700);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa nada sin la fecha de hoy")
    void noAvisaNadaSinLaFechaDeHoy() {
        cuandoSeEvaluaElNegocioSinLaFechaDeHoy();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("La huella no cambia con las ventas")
    void laHuellaNoCambiaConLasVentas() {
        dadoOtrasVentasDeLaMismaYerba();

        cuandoSeEvaluaElNegocioYElEscenarioBase();

        entoncesLaHuellaEsLaMismaQueLaDelEscenarioBase();
    }

    @Test
    @DisplayName("La huella cambia cuando cambia el precio")
    void laHuellaCambiaCuandoCambiaElPrecio() {
        dadoUnaYerbaDe(3450, 2700);

        cuandoSeEvaluaElNegocioYElEscenarioBase();

        entoncesLaHuellaDifiereDeLaDelEscenarioBase();
    }

    // --- dado ---

    private void dadoUnaYerbaQueVendeMasQueSuPromedioYEstaBajoElRangoDeLaZona() {
        yerba = yerba(3400, 2700);
    }

    private void dadoUnaYerbaDe(Object precio, Object costo) {
        yerba = yerba(precio, costo);
    }

    private void dadoUnaZonaConMinimoDe(int minimo) {
        zona = zona(minimo, 4800, 8);
    }

    private void dadoSoloUnaVentaDeVeinteUnidades() {
        ventas = List.of(venta(20, "2026-10-10"));
    }

    private void dadoUnaZonaQueSoloConoceOtroProducto() {
        zona = zonaConProducto("Fideos Luchetti 500 g", 8);
    }

    private void dadoUnaZonaDondeSoloDosComerciosVendenLaYerba() {
        zona = zonaConProducto(NOMBRE, 2);
    }

    private void dadoUnNegocioSinZona() {
        zona = AnalisisZona.sinZona();
    }

    private void dadoUnNegocioSinVentas() {
        ventas = List.of();
    }

    private void dadoOtrasVentasDeLaMismaYerba() {
        ventas = List.of(venta(10, "2026-08-20"), venta(25, "2026-10-10"));
    }

    // --- cuando ---

    private void cuandoSePideElTipo() {
        tipo = regla.tipo();
    }

    private void cuandoSeEvaluaElNegocio() {
        resultados = evaluar(zona, ventas, yerba);
    }

    private void cuandoSeEvaluaElNegocioSinLaFechaDeHoy() {
        ContextoNegocio contexto = new ContextoNegocio(AvisoTestData.NEGOCIO_ID,
                List.of(AvisoTestData.inventarioConPrecios(yerba)));
        resultados = regla.evaluar(contexto);
    }

    private void cuandoSeEvaluaElNegocioYElEscenarioBase() {
        cuandoSeEvaluaElNegocio();
        resultadosDelEscenarioBase = evaluar(zona(3500, 4800, 8), ventasEnAlza(), yerba(3400, 2700));
    }

    // --- entonces ---

    private void entoncesElTipoEsMuyVendido() {
        assertThat(tipo).isEqualTo(TipoAviso.MUY_VENDIDO);
    }

    private void entoncesAvisaQueLaYerbaSeVendeMasQueDeCostumbre() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.entidad()).isEqualTo("registro:" + YERBA_ID);
            assertThat(resultado.prioridad()).isEqualTo((short) 1);
            assertThat(resultado.proyectoId()).isEqualTo(AvisoTestData.PROYECTO_ID);
            assertThat(resultado.titulo())
                    .isEqualTo("Yerba Nobleza Gaucha 1 kg se vende más que de costumbre y lo tenés por debajo de tu zona");
        });
    }

    private void entoncesExplicaLasVentasElRangoYElMargen() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
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

    private void entoncesOfreceSubirElPrecioYAbrirElProducto() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.acciones()).hasSize(2);
            assertThat(resultado.acciones().get(0))
                    .isEqualTo(AccionesDePrueba.cambiarPrecio("Subir a $3.500", YERBA_ID, "3500"));
            assertThat(resultado.acciones().get(1)).isEqualTo(AccionesDePrueba.abrirProducto(YERBA_ID));
        });
    }

    private void entoncesLosMotivosNoHablanDelMargen() {
        assertThat(resultados).singleElement().satisfies(resultado -> assertThat(resultado.motivos()).hasSize(2));
    }

    private void entoncesElPrecioSugeridoEs(String esperado) {
        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.datos()).containsEntry("sugerido", new BigDecimal(esperado)));
    }

    private void entoncesNoAvisaNada() {
        assertThat(resultados).isEmpty();
    }

    private void entoncesLaHuellaEsLaMismaQueLaDelEscenarioBase() {
        assertThat(huellaDe(resultados)).isEqualTo(huellaDe(resultadosDelEscenarioBase));
    }

    private void entoncesLaHuellaDifiereDeLaDelEscenarioBase() {
        assertThat(huellaDe(resultados)).isNotEqualTo(huellaDe(resultadosDelEscenarioBase));
    }

    // --- datos ---

    private List<ResultadoRegla> evaluar(AnalisisZona zonaDelNegocio, List<Registro> ventasDelNegocio, Registro producto) {
        BloqueInventario inventario = AvisoTestData.inventarioConPrecios(producto);
        return regla.evaluar(new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY, List.of(inventario),
                List.of(AvisoTestData.ventas(ventasDelNegocio.toArray(new Registro[0]))), zonaDelNegocio));
    }

    private static String huellaDe(List<ResultadoRegla> resultados) {
        return resultados.get(0).huellaPara(TipoAviso.MUY_VENDIDO);
    }

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
