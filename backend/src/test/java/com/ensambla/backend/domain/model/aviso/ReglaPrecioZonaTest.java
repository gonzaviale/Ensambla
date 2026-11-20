package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.AccionesDePrueba;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReglaPrecioZonaTest {

    private static final UUID AZUCAR_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final String NOMBRE = "Azúcar Ledesma 1 kg";

    private final ReglaPrecioZona regla = new ReglaPrecioZona();

    private AnalisisZona zona = zona(10, 10);
    private Registro azucar;
    private TipoAviso tipo;
    private List<ResultadoRegla> resultados;
    private List<ResultadoRegla> resultadosDelEscenarioBase;

    @Test
    @DisplayName("Es la regla de los avisos de precio frente a la zona")
    void esLaReglaDeLosAvisosDePrecioFrenteALaZona() {
        cuandoSePideElTipo();

        entoncesElTipoEsPrecioZona();
    }

    @Test
    @DisplayName("Avisa del precio por debajo del rango de la zona y sugiere revisar costos y margen")
    void avisaDelPrecioPorDebajoDelRangoDeLaZonaYSugiereRevisarCostosYMargen() {
        dadoUnAzucarDe(800, 700);

        cuandoSeEvaluaElNegocio();

        entoncesAvisaQueElAzucarEstaMasBaratoQueEnLaZona();
        entoncesExplicaLaDiferenciaConElMinimoYElMargen();
        entoncesOfreceAbrirElProductoConLosDatosDelRango();
    }

    @Test
    @DisplayName("Cuando el costo supera al precio dice cuánto pierde por unidad")
    void cuandoElCostoSuperaAlPrecioDiceCuantoPierdePorUnidad() {
        dadoUnAzucarDe(800, 900);

        cuandoSeEvaluaElNegocio();

        entoncesElMotivoDiceQuePierdeCienPesosPorUnidad();
    }

    @Test
    @DisplayName("Sin costo cargado lo avisa en el motivo")
    void sinCostoCargadoLoAvisaEnElMotivo() {
        dadoUnAzucarDe(800, null);

        cuandoSeEvaluaElNegocio();

        entoncesElMotivoPideCargarElCosto();
    }

    @Test
    @DisplayName("Con el costo en cero lo avisa en el motivo")
    void conElCostoEnCeroLoAvisaEnElMotivo() {
        dadoUnAzucarDe(800, 0);

        cuandoSeEvaluaElNegocio();

        entoncesElMotivoPideCargarElCosto();
    }

    @Test
    @DisplayName("Avisa del precio por encima del rango y sugiere bajarlo al máximo de la zona")
    void avisaDelPrecioPorEncimaDelRangoYSugiereBajarloAlMaximoDeLaZona() {
        dadoUnAzucarDe(1500, null);

        cuandoSeEvaluaElNegocio();

        entoncesAvisaQueElAzucarEstaMasCaroQueEnLaZona();
        entoncesOfreceBajarElPrecioAlMaximoDeLaZona();
    }

    @Test
    @DisplayName("Si redondear el máximo a la decena no deja el precio por debajo del actual, sugiere el máximo exacto")
    void siRedondearElMaximoALaDecenaNoDejaElPrecioPorDebajoDelActualSugiereElMaximoExacto() {
        dadoUnaZonaConRangoDe("1000", "1296");
        dadoUnAzucarDe(1298, null);

        cuandoSeEvaluaElNegocio();

        entoncesSugiereBajarAlMaximoExactoDeMil296();
    }

    @Test
    @DisplayName("No avisa si el precio está dentro del rango")
    void noAvisaSiElPrecioEstaDentroDelRango() {
        dadoUnAzucarDe(1150, null);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa si el precio es el mínimo del rango")
    void noAvisaSiElPrecioEsElMinimoDelRango() {
        dadoUnAzucarDe(1000, null);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa si el precio es el máximo del rango")
    void noAvisaSiElPrecioEsElMaximoDelRango() {
        dadoUnAzucarDe(1300, null);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("Con menos de 3 comercios en la zona no avisa de un precio bajo")
    void conMenosDeTresComerciosEnLaZonaNoAvisaDeUnPrecioBajo() {
        dadoUnaZonaConDosComercios();
        dadoUnAzucarDe(800, null);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("Con menos de 3 comercios en la zona no avisa de un precio alto")
    void conMenosDeTresComerciosEnLaZonaNoAvisaDeUnPrecioAlto() {
        dadoUnaZonaConDosComercios();
        dadoUnAzucarDe(1500, null);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("Con exactamente 3 comercios sí avisa")
    void conExactamenteTresComerciosSiAvisa() {
        dadoUnaZonaConTresComerciosQueCobranElProducto();
        dadoUnAzucarDe(800, null);

        cuandoSeEvaluaElNegocio();

        entoncesAvisaUnaVez();
    }

    @Test
    @DisplayName("Con menos de 3 comercios que cobran el producto no compara contra él")
    void conMenosDeTresComerciosQueCobranElProductoNoCompararContraEl() {
        dadoUnaZonaDondeSoloDosComerciosCobranElProducto();
        dadoUnAzucarDe(800, null);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("Sin zona no avisa")
    void sinZonaNoAvisa() {
        dadoUnNegocioSinZona();
        dadoUnAzucarDe(800, null);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa si el producto no está en la zona")
    void noAvisaSiElProductoNoEstaEnLaZona() {
        dadoUnaZonaConOtroProducto("Yerba Playadito 1 kg");
        dadoUnAzucarDe(800, null);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("Reconoce el producto de la zona aunque cambien las tildes y las mayúsculas")
    void reconoceElProductoDeLaZonaAunqueCambienLasTildesYLasMayusculas() {
        dadoUnaZonaConOtroProducto("AZUCAR LEDESMA 1 KG");
        dadoUnAzucarDe(800, null);

        cuandoSeEvaluaElNegocio();

        entoncesAvisaUnaVez();
    }

    @Test
    @DisplayName("No avisa de un producto sin precio")
    void noAvisaDeUnProductoSinPrecio() {
        dadoUnAzucarDe(null, null);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa de un producto con el precio en cero")
    void noAvisaDeUnProductoConElPrecioEnCero() {
        dadoUnAzucarDe(0, null);

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("Avisa aunque el producto no tenga stock: el precio se compara igual")
    void avisaAunqueElProductoNoTengaStockElPrecioSeComparaIgual() {
        dadoUnAzucarSinStock();

        cuandoSeEvaluaElNegocio();

        entoncesAvisaUnaVez();
    }

    @Test
    @DisplayName("Nombra al producto sin nombre con un texto genérico")
    void nombraAlProductoSinNombreConUnTextoGenerico() {
        dadoUnProductoSinNombreQueLaZonaConoceComoUnProductoSinNombre();

        cuandoSeEvaluaElNegocio();

        entoncesElTituloEs("Un producto sin nombre está más barato que en tu zona");
    }

    @Test
    @DisplayName("Sólo muestra el rango agregado de la zona, sin datos de ningún comercio")
    void soloMuestraElRangoAgregadoDeLaZonaSinDatosDeNingunComercio() {
        dadoUnAzucarDe(800, 700);

        cuandoSeEvaluaElNegocio();

        entoncesSoloExponeElPrecioYElRangoAgregado();
    }

    @Test
    @DisplayName("La huella cambia cuando cambia el precio")
    void laHuellaCambiaCuandoCambiaElPrecio() {
        dadoUnAzucarDe(850, null);

        cuandoSeEvaluaElNegocioYElEscenarioBase();

        entoncesLaHuellaDifiereDeLaDelEscenarioBase();
    }

    @Test
    @DisplayName("La huella cambia cuando cambia el rango de la zona")
    void laHuellaCambiaCuandoCambiaElRangoDeLaZona() {
        dadoUnaZonaConRangoDe("1100", "1300");
        dadoUnAzucarDe(800, null);

        cuandoSeEvaluaElNegocioYElEscenarioBase();

        entoncesLaHuellaDifiereDeLaDelEscenarioBase();
    }

    @Test
    @DisplayName("La huella no cambia si no cambia nada")
    void laHuellaNoCambiaSiNoCambiaNada() {
        dadoUnAzucarDe(800, null);

        cuandoSeEvaluaElNegocioYElEscenarioBase();

        entoncesLaHuellaEsLaMismaQueLaDelEscenarioBase();
    }

    // --- dado ---

    private void dadoUnAzucarDe(Object precio, Object costo) {
        azucar = AvisoTestData.producto(AZUCAR_ID, NOMBRE, 25, precio, costo);
    }

    private void dadoUnAzucarSinStock() {
        azucar = AvisoTestData.producto(AZUCAR_ID, NOMBRE, 0, 800, null);
    }

    private void dadoUnProductoSinNombreQueLaZonaConoceComoUnProductoSinNombre() {
        zona = zonaConProducto("Un producto sin nombre", 10);
        azucar = AvisoTestData.producto(AZUCAR_ID, null, 5, 800, null);
    }

    private void dadoUnaZonaConRangoDe(String minimo, String maximo) {
        zona = new AnalisisZona("San Justo", 10, List.of(producto(NOMBRE, minimo, maximo, 10)));
    }

    private void dadoUnaZonaConDosComercios() {
        zona = zona(2, 10);
    }

    private void dadoUnaZonaConTresComerciosQueCobranElProducto() {
        zona = zona(3, 3);
    }

    private void dadoUnaZonaDondeSoloDosComerciosCobranElProducto() {
        zona = zona(10, 2);
    }

    private void dadoUnNegocioSinZona() {
        zona = AnalisisZona.sinZona();
    }

    private void dadoUnaZonaConOtroProducto(String nombre) {
        zona = zonaConProducto(nombre, 10);
    }

    // --- cuando ---

    private void cuandoSePideElTipo() {
        tipo = regla.tipo();
    }

    private void cuandoSeEvaluaElNegocio() {
        resultados = evaluar(zona, azucar);
    }

    private void cuandoSeEvaluaElNegocioYElEscenarioBase() {
        cuandoSeEvaluaElNegocio();
        resultadosDelEscenarioBase = evaluar(zona(10, 10), AvisoTestData.producto(AZUCAR_ID, NOMBRE, 25, 800, null));
    }

    // --- entonces ---

    private void entoncesElTipoEsPrecioZona() {
        assertThat(tipo).isEqualTo(TipoAviso.PRECIO_ZONA);
    }

    private void entoncesAvisaQueElAzucarEstaMasBaratoQueEnLaZona() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.entidad()).isEqualTo("registro:" + AZUCAR_ID);
            assertThat(resultado.prioridad()).isEqualTo((short) 2);
            assertThat(resultado.proyectoId()).isEqualTo(AvisoTestData.PROYECTO_ID);
            assertThat(resultado.titulo()).isEqualTo("Azúcar Ledesma 1 kg está más barato que en tu zona");
        });
    }

    private void entoncesExplicaLaDiferenciaConElMinimoYElMargen() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.detalle()).isEqualTo("Lo tenés a $800 y en tu zona se vende entre $1.000 y $1.300: "
                    + "$200 (20 %) menos que el mínimo. "
                    + "Revisá tus costos y tu ganancia: puede que estés vendiendo más barato de lo necesario.");
            assertThat(resultado.motivos()).containsExactly(
                    "En tu zona, 10 comercios lo venden entre $1.000 y $1.300.",
                    "Con tu costo de $700, te quedan $100 por unidad (14 % sobre el costo).");
        });
    }

    private void entoncesOfreceAbrirElProductoConLosDatosDelRango() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.acciones()).hasSize(1);
            assertThat(resultado.acciones().get(0)).isEqualTo(AccionesDePrueba.abrirProducto(AZUCAR_ID));
            assertThat(resultado.datos()).containsEntry("precio", new BigDecimal("800"))
                    .containsEntry("minimo", new BigDecimal("1000")).containsEntry("maximo", new BigDecimal("1300"));
        });
    }

    private void entoncesElMotivoDiceQuePierdeCienPesosPorUnidad() {
        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.motivos()).contains("Con tu costo de $900, perdés $100 por unidad."));
    }

    private void entoncesElMotivoPideCargarElCosto() {
        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(motivo(resultado, 1)).startsWith("Todavía no cargaste el costo"));
    }

    private void entoncesAvisaQueElAzucarEstaMasCaroQueEnLaZona() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.titulo()).isEqualTo("Azúcar Ledesma 1 kg está más caro que en tu zona");
            assertThat(resultado.detalle()).isEqualTo("Lo tenés a $1.500 y en tu zona se vende entre $1.000 y $1.300: "
                    + "$200 (15 %) más que el máximo. Bajarlo a $1.300 lo deja dentro del rango y evita que pierdas ventas.");
            assertThat(resultado.motivos()).containsExactly(
                    "En tu zona, 10 comercios lo venden entre $1.000 y $1.300.",
                    "Los clientes comparan: por encima de lo que cobran al lado se compra menos.");
        });
    }

    private void entoncesOfreceBajarElPrecioAlMaximoDeLaZona() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.acciones()).hasSize(2);
            assertThat(resultado.acciones().get(0))
                    .isEqualTo(AccionesDePrueba.cambiarPrecio("Bajar a $1.300", AZUCAR_ID, "1300"));
            assertThat(resultado.acciones().get(1)).isEqualTo(AccionesDePrueba.abrirProducto(AZUCAR_ID));
        });
    }

    private void entoncesSugiereBajarAlMaximoExactoDeMil296() {
        assertThat(resultados).singleElement().satisfies(resultado -> assertThat(resultado.acciones().get(0))
                .isEqualTo(AccionesDePrueba.cambiarPrecio("Bajar a $1.296", AZUCAR_ID, "1296")));
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

    private void entoncesSoloExponeElPrecioYElRangoAgregado() {
        ResultadoRegla resultado = resultados.get(0);
        assertThat(resultado.datos().keySet()).containsExactly("precio", "minimo", "maximo");
        assertThat(resultado.detalle() + resultado.motivos()).doesNotContain("promedio");
    }

    private void entoncesLaHuellaDifiereDeLaDelEscenarioBase() {
        assertThat(huellaDe(resultados)).isNotEqualTo(huellaDe(resultadosDelEscenarioBase));
    }

    private void entoncesLaHuellaEsLaMismaQueLaDelEscenarioBase() {
        assertThat(huellaDe(resultados)).isEqualTo(huellaDe(resultadosDelEscenarioBase));
    }

    // --- datos ---

    private List<ResultadoRegla> evaluar(AnalisisZona zonaDelNegocio, Registro producto) {
        BloqueInventario inventario = AvisoTestData.inventarioConPrecios(producto);
        return regla.evaluar(new ContextoNegocio(AvisoTestData.NEGOCIO_ID, null, List.of(inventario), List.of(),
                zonaDelNegocio));
    }

    private static String huellaDe(List<ResultadoRegla> resultados) {
        return resultados.get(0).huellaPara(TipoAviso.PRECIO_ZONA);
    }

    private static String motivo(ResultadoRegla resultado, int indice) {
        return resultado.motivos().get(indice).toString();
    }

    /** Rango de $1.000 a $1.300 con los comercios de la zona y los que cobran el producto que se indican. */
    private static AnalisisZona zona(long comerciosZona, int comerciosProducto) {
        return new AnalisisZona("San Justo", comerciosZona, List.of(producto(NOMBRE, "1000", "1300", comerciosProducto)));
    }

    private static AnalisisZona zonaConProducto(String nombre, int comerciosProducto) {
        return new AnalisisZona("San Justo", 10, List.of(producto(nombre, "1000", "1300", comerciosProducto)));
    }

    private static AgregadoZonaProducto producto(String nombre, String minimo, String maximo, int comercios) {
        return new AgregadoZonaProducto(nombre, comercios, comercios, new BigDecimal(minimo), new BigDecimal("1147"),
                new BigDecimal(maximo), new BigDecimal("3"));
    }
}
