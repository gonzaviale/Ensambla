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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReglaProductoEstancadoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final UUID AZUCAR_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID YERBA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID COCA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000003");
    private static final String NOMBRE = "Azúcar Ledesma 1 kg";
    private static final String TITULO_SIN_NOMBRE = "Un producto sin nombre está caro y no se mueve";

    private final ReglaProductoEstancado regla = new ReglaProductoEstancado(
            new MetricasVentas(30, new BigDecimal("0.5")));

    private AnalisisZona zona = AnalisisZona.sinZona();
    private List<Registro> ventas = ventasDelResto();
    private Registro azucar = azucar(25, 1500);

    private TipoAviso tipo;
    private List<ResultadoRegla> resultados;
    private List<ResultadoRegla> resultadosDelEscenarioBase;

    @Test
    @DisplayName("Es la regla de los avisos de producto estancado")
    void esLaReglaDeLosAvisosDeProductoEstancado() {
        cuandoSePideElTipo();

        entoncesElTipoEsEstancado();
    }

    @Test
    @DisplayName("Avisa del producto caro que no se vende y sugiere el precio promedio al que lo vendía")
    void avisaDelProductoCaroQueNoSeVendeYSugiereElPrecioPromedioAlQueLoVendia() {
        dadoUnaVentaVieja();

        cuandoSeEvaluaElNegocio();

        entoncesAvisaQueElAzucarEstaCaroYNoSeMueve();
        entoncesSugiereElPrecioHistorico();
    }

    @Test
    @DisplayName("Ofrece rebajar el precio con una acción que edita el registro y otra que abre el producto")
    void ofreceRebajarElPrecioConUnaAccionQueEditaElRegistroYOtraQueAbreElProducto() {
        dadoUnaVentaVieja();

        cuandoSeEvaluaElNegocio();

        entoncesOfreceRebajarElPrecioYAbrirElProducto();
    }

    @Test
    @DisplayName("Con ventas en el período lo cuenta en el detalle")
    void conVentasEnElPeriodoLoCuentaEnElDetalle() {
        dadoTresUnidadesVendidasEnElPeriodo();

        cuandoSeEvaluaElNegocio();

        entoncesElDetalleCuentaLasTresUnidadesYLaUltimaVenta();
    }

    @Test
    @DisplayName("Avisa del producto más caro que la zona y sugiere el promedio de la zona redondeado a la decena")
    void avisaDelProductoMasCaroQueLaZonaYSugiereElPromedioDeLaZonaRedondeadoALaDecena() {
        dadoUnaZonaQueVendeElAzucarAPromedioDe(NOMBRE, "1147");

        cuandoSeEvaluaElNegocio();

        entoncesSugiereElPrecioPromedioDeLaZona();
    }

    @Test
    @DisplayName("Prefiere la referencia de la zona cuando el historial propio también dice que está caro")
    void prefiereLaReferenciaDeLaZonaCuandoElHistorialPropioTambienDiceQueEstaCaro() {
        dadoUnaVentaVieja();
        dadoUnaZonaQueVendeElAzucarAPromedioDe(NOMBRE, "1147");

        cuandoSeEvaluaElNegocio();

        entoncesElPrimerMotivoEmpiezaCon("En tu zona");
    }

    @Test
    @DisplayName("Usa el historial propio cuando el precio no supera al de la zona")
    void usaElHistorialPropioCuandoElPrecioNoSuperaAlDeLaZona() {
        dadoUnaVentaVieja();
        dadoUnaZonaQueVendeElAzucarAPromedioDe(NOMBRE, "1450");

        cuandoSeEvaluaElNegocio();

        entoncesElPrimerMotivoEmpiezaCon("Está por encima de tu precio promedio histórico");
    }

    @Test
    @DisplayName("Reconoce el producto de la zona aunque cambien las tildes y las mayúsculas")
    void reconoceElProductoDeLaZonaAunqueCambienLasTildesYLasMayusculas() {
        dadoUnaZonaQueVendeElAzucarAPromedioDe("AZUCAR LEDESMA 1 KG", "1147");

        cuandoSeEvaluaElNegocio();

        entoncesAvisaUnaVez();
    }

    @Test
    @DisplayName("No avisa de un producto caro que se vende normalmente")
    void noAvisaDeUnProductoCaroQueSeVendeNormalmente() {
        dadoVentasNormalesDelAzucar();
        dadoUnaZonaQueVendeElAzucarAPromedioDe(NOMBRE, "1147");

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa nada si el negocio no anotó ventas")
    void noAvisaNadaSiElNegocioNoAnotoVentas() {
        dadoUnNegocioSinVentas();
        dadoUnaZonaQueVendeElAzucarAPromedioDe(NOMBRE, "1147");

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
    @DisplayName("No avisa si el precio no supera en más de un 5 % al de referencia")
    void noAvisaSiElPrecioNoSuperaEnMasDeUnCincoPorCientoAlDeReferencia() {
        dadoUnaVentaAlPrecioDe1500();
        dadoUnAzucarDe(25, 1575);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa si el producto no está en la zona ni tiene precio histórico")
    void noAvisaSiElProductoNoEstaEnLaZonaNiTienePrecioHistorico() {
        dadoUnaZonaQueVendeElAzucarAPromedioDe("Yerba Playadito 1 kg", "1000");

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa si el precio sugerido redondeado no queda por debajo del actual")
    void noAvisaSiElPrecioSugeridoRedondeadoNoQuedaPorDebajoDelActual() {
        dadoUnaZonaQueVendeElAzucarAPromedioDe(NOMBRE, "15");
        dadoUnAzucarDe(25, 16);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa si el precio sugerido redondeado queda en cero")
    void noAvisaSiElPrecioSugeridoRedondeadoQuedaEnCero() {
        dadoUnaZonaQueVendeElAzucarAPromedioDe(NOMBRE, "4");
        dadoUnAzucarDe(25, 5);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa de productos sin stock")
    void noAvisaDeProductosSinStock() {
        dadoUnaZonaQueVendeElAzucarAPromedioDe(NOMBRE, "1147");
        dadoUnAzucarDe(0, 1500);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa de productos sin cantidad cargada")
    void noAvisaDeProductosSinCantidadCargada() {
        dadoUnaZonaQueVendeElAzucarAPromedioDe(NOMBRE, "1147");
        dadoUnAzucarDe(null, 1500);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa de productos sin precio")
    void noAvisaDeProductosSinPrecio() {
        dadoUnaZonaQueVendeElAzucarAPromedioDe(NOMBRE, "1147");
        dadoUnAzucarDe(25, null);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa de productos con precio en cero")
    void noAvisaDeProductosConPrecioEnCero() {
        dadoUnaZonaQueVendeElAzucarAPromedioDe(NOMBRE, "1147");
        dadoUnAzucarDe(25, 0);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("Nombra al producto sin nombre con un texto genérico")
    void nombraAlProductoSinNombreConUnTextoGenerico() {
        dadoUnaVentaVieja();
        dadoUnProductoCaroConNombre(null);

        cuandoSeEvaluaElNegocio();

        entoncesElTituloEs(TITULO_SIN_NOMBRE);
    }

    @Test
    @DisplayName("Nombra al producto de nombre en blanco con un texto genérico")
    void nombraAlProductoDeNombreEnBlancoConUnTextoGenerico() {
        dadoUnaVentaVieja();
        dadoUnProductoCaroConNombre("   ");

        cuandoSeEvaluaElNegocio();

        entoncesElTituloEs(TITULO_SIN_NOMBRE);
    }

    @Test
    @DisplayName("Escribe los precios con decimales con coma")
    void escribeLosPreciosConDecimalesConComa() {
        dadoUnaVentaVieja();
        dadoUnAzucarDe(25, 1500.5);

        cuandoSeEvaluaElNegocio();

        entoncesElDetalleContiene("lo tenés a $1.500,5.");
    }

    @Test
    @DisplayName("La huella no cambia cuando sólo cambian las ventas")
    void laHuellaNoCambiaCuandoSoloCambianLasVentas() {
        dadoUnaZonaQueVendeElAzucarAPromedioDe(NOMBRE, "1147");
        dadoUnaVentaAntigua();

        cuandoSeEvaluaElNegocioYElEscenarioBase();

        entoncesLaHuellaEsLaMismaQueLaDelEscenarioBase();
    }

    @Test
    @DisplayName("La huella cambia cuando cambia el precio del producto")
    void laHuellaCambiaCuandoCambiaElPrecioDelProducto() {
        dadoUnaZonaQueVendeElAzucarAPromedioDe(NOMBRE, "1147");
        dadoUnAzucarDe(25, 1600);

        cuandoSeEvaluaElNegocioYElEscenarioBase();

        entoncesLaHuellaDifiereDeLaDelEscenarioBase();
    }

    // --- dado ---

    private void dadoUnaVentaVieja() {
        ventas.add(venta(AZUCAR_ID, 2, 1000, "2026-08-01"));
    }

    private void dadoUnaVentaAntigua() {
        ventas.add(venta(AZUCAR_ID, 2, 1150, "2026-08-01"));
    }

    private void dadoUnaVentaAlPrecioDe1500() {
        ventas.add(venta(AZUCAR_ID, 2, 1500, "2026-08-01"));
    }

    private void dadoTresUnidadesVendidasEnElPeriodo() {
        ventas.add(venta(AZUCAR_ID, 3, 1000, "2026-10-10"));
    }

    private void dadoVentasNormalesDelAzucar() {
        ventas.add(venta(AZUCAR_ID, 20, 1500, "2026-10-10"));
    }

    private void dadoUnNegocioSinVentas() {
        ventas = new ArrayList<>();
    }

    private void dadoUnaZonaQueVendeElAzucarAPromedioDe(String nombre, String promedio) {
        zona = zonaConAzucar(nombre, promedio);
    }

    private void dadoUnAzucarDe(Object cantidad, Object precio) {
        azucar = azucar(cantidad, precio);
    }

    private void dadoUnProductoCaroConNombre(String nombre) {
        azucar = AvisoTestData.producto(AZUCAR_ID, nombre, 25, 1500, null);
    }

    // --- cuando ---

    private void cuandoSePideElTipo() {
        tipo = regla.tipo();
    }

    private void cuandoSeEvaluaElNegocio() {
        resultados = evaluar(zona, ventas, azucar);
    }

    private void cuandoSeEvaluaElNegocioSinLaFechaDeHoy() {
        ContextoNegocio contexto = new ContextoNegocio(AvisoTestData.NEGOCIO_ID,
                List.of(AvisoTestData.inventarioConPrecios(azucar)));
        resultados = regla.evaluar(contexto);
    }

    private void cuandoSeEvaluaElNegocioYElEscenarioBase() {
        cuandoSeEvaluaElNegocio();
        resultadosDelEscenarioBase = evaluar(zonaConAzucar(NOMBRE, "1147"), ventasDelResto(), azucar(25, 1500));
    }

    // --- entonces ---

    private void entoncesElTipoEsEstancado() {
        assertThat(tipo).isEqualTo(TipoAviso.ESTANCADO);
    }

    private void entoncesAvisaQueElAzucarEstaCaroYNoSeMueve() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.entidad()).isEqualTo("registro:" + AZUCAR_ID);
            assertThat(resultado.prioridad()).isEqualTo((short) 2);
            assertThat(resultado.titulo()).isEqualTo("Azúcar Ledesma 1 kg está caro y no se mueve");
            assertThat(resultado.proyectoId()).isEqualTo(AvisoTestData.PROYECTO_ID);
        });
    }

    private void entoncesSugiereElPrecioHistorico() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.detalle()).isEqualTo("No vendiste ninguno en los últimos 30 días y lo tenés a $1.500. "
                    + "Si lo bajás a $1.000, queda cerca de lo que solías cobrar.");
            assertThat(resultado.motivos()).containsExactly(
                    "Está por encima de tu precio promedio histórico de $1.000.",
                    "La última venta fue el 1/8.",
                    "Tenés 25 en stock.");
        });
    }

    private void entoncesOfreceRebajarElPrecioYAbrirElProducto() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.acciones()).hasSize(2);
            assertThat(resultado.acciones().get(0))
                    .isEqualTo(AccionesDePrueba.cambiarPrecio("Rebajar a $1.000", AZUCAR_ID, "1000"));
            assertThat(resultado.acciones().get(1)).isEqualTo(AccionesDePrueba.abrirProducto(AZUCAR_ID));
        });
    }

    private void entoncesElDetalleCuentaLasTresUnidadesYLaUltimaVenta() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.detalle()).startsWith("Vendiste 3 en los últimos 30 días y lo tenés a $1.500.");
            assertThat(resultado.motivos()).contains("La última venta fue el 10/10.");
        });
    }

    private void entoncesSugiereElPrecioPromedioDeLaZona() {
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

    private void entoncesElPrimerMotivoEmpiezaCon(String texto) {
        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.motivos().get(0).toString()).startsWith(texto));
    }

    private void entoncesNoAvisaNada() {
        assertThat(resultados).isEmpty();
    }

    private void entoncesAvisaUnaVez() {
        assertThat(resultados).hasSize(1);
    }

    private void entoncesElTituloEs(String esperado) {
        assertThat(resultados).singleElement().satisfies(resultado -> assertThat(resultado.titulo()).isEqualTo(esperado));
    }

    private void entoncesElDetalleContiene(String texto) {
        assertThat(resultados).singleElement().satisfies(resultado -> assertThat(resultado.detalle()).contains(texto));
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
        return resultados.get(0).huellaPara(TipoAviso.ESTANCADO);
    }

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

    private static AnalisisZona zonaConAzucar(String nombre, String promedio) {
        AgregadoZonaProducto producto = new AgregadoZonaProducto(nombre, 10, 9, new BigDecimal("1000"),
                new BigDecimal(promedio), new BigDecimal("1300"), new BigDecimal("3"));
        return new AnalisisZona("San Justo", 10, List.of(producto));
    }
}
