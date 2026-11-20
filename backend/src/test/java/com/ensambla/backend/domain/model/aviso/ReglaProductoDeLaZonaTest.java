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
import java.util.Arrays;
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
    private ContextoNegocio contexto;
    private List<ResultadoRegla> avisos;

    @Test
    @DisplayName("Es la regla de los avisos de consumo de la zona")
    void esLaReglaDeLosAvisosDeConsumoDeLaZona() {
        entoncesLaReglaEsDeTipo(TipoAviso.CONSUMO_ZONA);
    }

    @Test
    @DisplayName("Sugiere los productos más vendidos de la zona que el negocio no tiene, y no los que ya tiene")
    void sugiereLosMasVendidosDeLaZonaQueElNegocioNoTiene() {
        dadoUnNegocioEnLaZona(unaZonaDeDiezComercios(), conLosProductos(YERBA, FIDEOS));

        cuandoSeEvaluaLaRegla();

        entoncesLosTitulosSon(
                "En tu zona se vende Café La Virginia 250 g y no lo tenés",
                "En tu zona se vende Jabón en polvo Skip 800 g y no lo tenés");
    }

    @Test
    @DisplayName("Cuenta con sólo los agregados de la zona, sin identificar a ningún comercio")
    void cuentaConSoloLosAgregadosDeLaZona() {
        dadoUnNegocioEnLaZona(unaZonaDeDiezComercios(), conLosProductos(YERBA));

        cuandoSeEvaluaLaRegla();

        entoncesElPrimerAvisoEsSobreElProductoDeLaZona(CAFE);
        entoncesElPrimerAvisoTienePrioridad((short) 1);
        entoncesElPrimerAvisoEsDelProyecto();
        entoncesElPrimerAvisoDetalla("8 de 10 comercios lo venden, a $3.200 en promedio. "
                + "Cada uno vende unas 9 unidades por semana.");
        entoncesLosMotivosDelPrimerAvisoSon(
                "Es un dato agregado de varios comercios de tu zona: nadie queda identificado.",
                "Lo cruzamos con tu stock y no encontramos nada parecido.");
        entoncesElPrimerAvisoGuardaSoloElNombreYElPrecio(CAFE, new BigDecimal("3200"));
    }

    @Test
    @DisplayName("Ofrece sumarlo al inventario y ver el producto en la zona")
    void ofreceSumarloAlInventarioYVerElProductoEnLaZona() {
        dadoUnNegocioEnLaZona(unaZonaDeDiezComercios(), conLosProductos(YERBA));

        cuandoSeEvaluaLaRegla();

        entoncesLasAccionesDelPrimerAvisoSon(
                AccionesDePrueba.crearEnElInventario("Sumar a mi stock",
                        Map.of("name", CAFE, "quantity", 0, "price", new BigDecimal("3200"), "minimum", 5)),
                AccionesDePrueba.verEnLaZona(CAFE));
    }

    @Test
    @DisplayName("Sugiere como precio el promedio de la zona redondeado a la decena")
    void sugiereComoPrecioElPromedioRedondeadoALaDecena() {
        dadoUnNegocioEnLaZona(unaZonaDe(10, unProductoDeLaZona(CAFE, 8, "3147", "9")), conLosProductos(YERBA));

        cuandoSeEvaluaLaRegla();

        entoncesElUnicoAvisoSugiereElPrecio(new BigDecimal("3150"));
    }

    @Test
    @DisplayName("Avisa de a lo sumo el máximo de productos, los más vendidos primero")
    void avisaDeALoSumoElMaximoDeProductosLosMasVendidosPrimero() {
        dadoUnNegocioEnLaZona(unaZonaConCuatroProductosPopulares(), conLosProductos(FIDEOS));

        cuandoSeEvaluaLaRegla();

        entoncesLosTitulosSon(
                "En tu zona se vende Yerba Nobleza Gaucha 1 kg y no lo tenés",
                "En tu zona se vende Café La Virginia 250 g y no lo tenés");
    }

    @Test
    @DisplayName("Reconoce el mismo producto aunque cambien la marca y el tamaño")
    void reconoceElMismoProductoAunqueCambienLaMarcaYElTamanio() {
        dadoUnNegocioEnLaZona(unaZonaDondeSeVendeCafe(), conLosProductos("Café Cabrales 500 g"));

        cuandoSeEvaluaLaRegla();

        entoncesNoHayAvisos();
    }

    @Test
    @DisplayName("Reconoce el mismo producto aunque tenga palabras de relleno")
    void reconoceElMismoProductoAunqueTengaPalabrasDeRelleno() {
        dadoUnNegocioEnLaZona(unaZonaDondeSeVendeCafe(), conLosProductos("Con Café Cabrales"));

        cuandoSeEvaluaLaRegla();

        entoncesNoHayAvisos();
    }

    @Test
    @DisplayName("Reconoce el mismo producto aunque esté en mayúsculas y sin marca")
    void reconoceElMismoProductoAunqueEsteEnMayusculasYSinMarca() {
        dadoUnNegocioEnLaZona(unaZonaDondeSeVendeCafe(), conLosProductos("CAFÉ"));

        cuandoSeEvaluaLaRegla();

        entoncesNoHayAvisos();
    }

    @Test
    @DisplayName("Mira los productos de todas las tablas de inventario del negocio")
    void miraLosProductosDeTodasLasTablasDeInventario() {
        dadoUnNegocioConDosInventarios(conLosProductos(FIDEOS), conLosProductos("Café Cabrales 500 g"));

        cuandoSeEvaluaLaRegla();

        entoncesNingunTituloEs("En tu zona se vende Café La Virginia 250 g y no lo tenés");
    }

    @Test
    @DisplayName("Un producto sin nombre, en blanco o sin palabras que lo distingan no tapa a ninguno de la zona")
    void unProductoSinNombreDistintivoNoTapaANingunoDeLaZona() {
        dadoUnNegocioEnLaZona(unaZonaDondeSeVendeCafe(), productosSinNombreDistintivo());

        cuandoSeEvaluaLaRegla();

        entoncesHayAvisos(1);
    }

    @Test
    @DisplayName("No avisa de lo que vende menos del 60 % de los comercios de la zona")
    void noAvisaDeLoQueVendeMenosDelSesentaPorCientoDeLosComercios() {
        dadoUnNegocioEnLaZona(unaZonaDe(10, unProductoDeLaZona(CAFE, 5, "3200", "9")), conLosProductos(YERBA));

        cuandoSeEvaluaLaRegla();

        entoncesNoHayAvisos();
    }

    @Test
    @DisplayName("Avisa de lo que vende justo el 60 % de los comercios de la zona")
    void avisaDeLoQueVendeJustoElSesentaPorCientoDeLosComercios() {
        dadoUnNegocioEnLaZona(unaZonaDe(10, unProductoDeLaZona(CAFE, 6, "3200", "9")), conLosProductos(YERBA));

        cuandoSeEvaluaLaRegla();

        entoncesHayAvisos(1);
    }

    @Test
    @DisplayName("Sin ranking no avisa: con menos de 3 comercios en la zona")
    void sinRankingNoAvisaConMenosDeTresComerciosEnLaZona() {
        dadoUnNegocioEnLaZona(unaZonaDe(2, unProductoDeLaZona(CAFE, 3, "3200", "9")), conLosProductos(YERBA));

        cuandoSeEvaluaLaRegla();

        entoncesNoHayAvisos();
    }

    @Test
    @DisplayName("Sin ranking no avisa: con menos de 3 comercios que vendan el producto")
    void sinRankingNoAvisaConMenosDeTresComerciosQueLoVendan() {
        dadoUnNegocioEnLaZona(unaZonaDe(3, unProductoDeLaZona(CAFE, 2, "3200", "9")), conLosProductos(YERBA));

        cuandoSeEvaluaLaRegla();

        entoncesNoHayAvisos();
    }

    @Test
    @DisplayName("Sin zona no avisa")
    void sinZonaNoAvisa() {
        dadoUnNegocioEnLaZona(AnalisisZona.sinZona(), conLosProductos(YERBA));

        cuandoSeEvaluaLaRegla();

        entoncesNoHayAvisos();
    }

    @Test
    @DisplayName("Sin tabla de inventario donde sumarlo no avisa")
    void sinTablaDeInventarioDondeSumarloNoAvisa() {
        dadoUnNegocioSinInventarioEnLaZona(unaZonaDeDiezComercios());

        cuandoSeEvaluaLaRegla();

        entoncesNoHayAvisos();
    }

    @Test
    @DisplayName("La huella no cambia con los comercios ni las unidades")
    void laHuellaNoCambiaConLosComerciosNiLasUnidades() {
        ResultadoRegla base = cuandoSeAvisaDelCafeEn(unaZonaDe(10, unProductoDeLaZona(CAFE, 8, "3200", "9")));
        ResultadoRegla masComercios = cuandoSeAvisaDelCafeEn(unaZonaDe(12, unProductoDeLaZona(CAFE, 10, "3200", "15")));

        entoncesTienenLaMismaHuella(base, masComercios);
    }

    @Test
    @DisplayName("La huella cambia con el precio sugerido")
    void laHuellaCambiaConElPrecioSugerido() {
        ResultadoRegla base = cuandoSeAvisaDelCafeEn(unaZonaDe(10, unProductoDeLaZona(CAFE, 8, "3200", "9")));
        ResultadoRegla otroPrecio = cuandoSeAvisaDelCafeEn(unaZonaDe(10, unProductoDeLaZona(CAFE, 8, "3400", "9")));

        entoncesTienenDistintaHuella(base, otroPrecio);
    }

    private static Registro[] conLosProductos(String... nombres) {
        return Arrays.stream(nombres).map(ReglaProductoDeLaZonaTest::unProducto).toArray(Registro[]::new);
    }

    private static Registro unProducto(String nombre) {
        return AvisoTestData.producto(UUID.randomUUID(), nombre, 10, 1000, null);
    }

    private static Registro[] productosSinNombreDistintivo() {
        return new Registro[]{AvisoTestData.producto(UUID.randomUUID(), null, 5, 100, null),
                unProducto("   "), unProducto("500 g")};
    }

    /** Zona de 10 comercios: la yerba la vende 9, el café 8, el jabón 7, los fideos 5 (50 %) y la sal 2. */
    private static AnalisisZona unaZonaDeDiezComercios() {
        return unaZonaDe(10,
                unProductoDeLaZona(FIDEOS, 5, "1300", "12"), unProductoDeLaZona(JABON, 7, "3500", "6"),
                unProductoDeLaZona(CAFE, 8, "3200", "9"), unProductoDeLaZona(YERBA, 9, "4200", "20"),
                unProductoDeLaZona("Sal Celusal 500 g", 2, "900", "30"));
    }

    private static AnalisisZona unaZonaConCuatroProductosPopulares() {
        return unaZonaDe(10,
                unProductoDeLaZona(AZUCAR, 7, "1150", "3"), unProductoDeLaZona(JABON, 7, "3500", "6"),
                unProductoDeLaZona(CAFE, 8, "3200", "9"), unProductoDeLaZona(YERBA, 9, "4200", "20"));
    }

    private static AnalisisZona unaZonaDondeSeVendeCafe() {
        return unaZonaDe(10, unProductoDeLaZona(CAFE, 8, "3200", "9"));
    }

    private static AnalisisZona unaZonaDe(long comercios, AgregadoZonaProducto... productos) {
        return new AnalisisZona("San Justo", comercios, List.of(productos));
    }

    private static AgregadoZonaProducto unProductoDeLaZona(
            String nombre, int comerciosVenden, String promedio, String semana) {
        return new AgregadoZonaProducto(nombre, 8, comerciosVenden, new BigDecimal("100"), new BigDecimal(promedio),
                new BigDecimal("9999"), new BigDecimal(semana));
    }

    private void dadoUnNegocioEnLaZona(AnalisisZona zona, Registro... productos) {
        contexto = new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY,
                List.of(AvisoTestData.inventarioConPrecios(productos)), List.of(), zona);
    }

    private void dadoUnNegocioConDosInventarios(Registro[] primero, Registro[] segundo) {
        contexto = new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY,
                List.of(AvisoTestData.inventarioConPrecios(primero), AvisoTestData.inventarioConPrecios(segundo)),
                List.of(), unaZonaDeDiezComercios());
    }

    private void dadoUnNegocioSinInventarioEnLaZona(AnalisisZona zona) {
        contexto = new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY, List.of(), List.of(), zona);
    }

    private void cuandoSeEvaluaLaRegla() {
        avisos = regla.evaluar(contexto);
    }

    private ResultadoRegla cuandoSeAvisaDelCafeEn(AnalisisZona zona) {
        dadoUnNegocioEnLaZona(zona, conLosProductos(YERBA));
        cuandoSeEvaluaLaRegla();
        return avisos.getFirst();
    }

    private void entoncesLaReglaEsDeTipo(TipoAviso tipo) {
        assertThat(regla.tipo()).isEqualTo(tipo);
    }

    private void entoncesNoHayAvisos() {
        assertThat(avisos).isEmpty();
    }

    private void entoncesHayAvisos(int cantidad) {
        assertThat(avisos).hasSize(cantidad);
    }

    private void entoncesLosTitulosSon(String... titulos) {
        assertThat(avisos).extracting(ResultadoRegla::titulo).containsExactly(titulos);
    }

    private void entoncesNingunTituloEs(String titulo) {
        assertThat(avisos).extracting(ResultadoRegla::titulo).doesNotContain(titulo);
    }

    private void entoncesElPrimerAvisoEsSobreElProductoDeLaZona(String nombre) {
        assertThat(avisos.getFirst().entidad()).isEqualTo(ResultadoRegla.entidadDeZona(nombre));
    }

    private void entoncesElPrimerAvisoTienePrioridad(short prioridad) {
        assertThat(avisos.getFirst().prioridad()).isEqualTo(prioridad);
    }

    private void entoncesElPrimerAvisoEsDelProyecto() {
        assertThat(avisos.getFirst().proyectoId()).isEqualTo(AvisoTestData.PROYECTO_ID);
    }

    private void entoncesElPrimerAvisoDetalla(String detalle) {
        assertThat(avisos.getFirst().detalle()).isEqualTo(detalle);
    }

    private void entoncesLosMotivosDelPrimerAvisoSon(String... motivos) {
        assertThat(avisos.getFirst().motivos()).containsExactly(motivos);
    }

    private void entoncesElPrimerAvisoGuardaSoloElNombreYElPrecio(String nombre, BigDecimal precio) {
        assertThat(avisos.getFirst().datos()).containsOnlyKeys("nombre", "precio")
                .containsEntry("nombre", nombre).containsEntry("precio", precio);
    }

    private void entoncesLasAccionesDelPrimerAvisoSon(AccionDeAviso... acciones) {
        assertThat(avisos.getFirst().acciones()).containsExactly(acciones);
    }

    private void entoncesElUnicoAvisoSugiereElPrecio(BigDecimal precio) {
        assertThat(avisos).hasSize(1);
        assertThat(avisos.getFirst().datos()).containsEntry("precio", precio);
    }

    private void entoncesTienenLaMismaHuella(ResultadoRegla uno, ResultadoRegla otro) {
        assertThat(otro.huellaPara(TipoAviso.CONSUMO_ZONA)).isEqualTo(uno.huellaPara(TipoAviso.CONSUMO_ZONA));
    }

    private void entoncesTienenDistintaHuella(ResultadoRegla uno, ResultadoRegla otro) {
        assertThat(otro.huellaPara(TipoAviso.CONSUMO_ZONA)).isNotEqualTo(uno.huellaPara(TipoAviso.CONSUMO_ZONA));
    }
}
