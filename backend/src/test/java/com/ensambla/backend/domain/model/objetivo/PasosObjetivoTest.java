package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PasosObjetivoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 9);
    private static final RegistroId YERBA = id("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId CACAO = id("d0000000-0000-4000-8000-000000000002");
    private static final RegistroId ALFAJOR = id("d0000000-0000-4000-8000-000000000003");

    private final MetricasVentas metricasVentas = new MetricasVentas(30, new BigDecimal("0.5"));

    private List<DatosDeProyecto> sistemas = List.of();
    private AnalisisZona zona = AnalisisZona.sinZona();
    private List<PasoObjetivo> pasos;

    @Test
    @DisplayName("Elige los tres pasos en orden, sin repetir productos y con el rango anónimo de la zona")
    void eligeLosTresPasosEnOrdenSinRepetirProductosYConElRangoAnonimoDeLaZona() {
        dadoUnAlmacenConYerbaCacaoYAlfajorConVentasDeYerbaYAlfajor();
        dadoUnaZonaQueConoceLaYerbaEntre3500Y4300();

        cuandoSeEligenLosPasos();

        entoncesSeEligenLosTresPasosEnOrdenSinRepetirProductos();
        entoncesLaYerbaTraeElRangoAnonimoDeLaZonaYElCacaoNo();
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando no hay inventarios")
    void devuelveUnaListaVaciaCuandoNoHayInventarios() {
        dadoUnSistemaSinInventarios();

        cuandoSeEligenLosPasos();

        entoncesNoHayPasos();
    }

    @Test
    @DisplayName("Ignora productos sin nombre, precio o costo numérico y positivo")
    void ignoraProductosSinNombrePrecioOCostoNumericoYPositivo() {
        dadoProductosSinNombrePrecioOCostoValidos();

        cuandoSeEligenLosPasos();

        entoncesNoHayPasos();
    }

    @Test
    @DisplayName("Un producto sin stock numérico no se propone como mercadería parada")
    void unProductoSinStockNumericoNoSePropoeComoMercaderiaParada() {
        dadoUnProductoSinStockNumerico();

        cuandoSeEligenLosPasos();

        entoncesNoHayPasos();
    }

    @Test
    @DisplayName("Exige dos pasos de margen para rebajar productos baratos y caros")
    void exigeDosPasosDeMargenParaRebajarProductosBaratosYCaros() {
        dadoProductosBaratosYCarosConMargenJustoEInsuficiente();

        cuandoSeEligenLosPasos();

        entoncesSoloSeProponeMoverElCaroConMargenJusto();
    }

    @Test
    @DisplayName("Resuelve empates por nombre y no publica zona con menos de tres comercios")
    void resuelveEmpatesPorNombreYNoPublicaZonaConMenosDeTresComercios() {
        dadoDosProductosEmpatadosYUnaZonaConDosComercios();

        cuandoSeEligenLosPasos();

        entoncesSeDesempataPorNombreSinPublicarLaZona();
    }

    @Test
    @DisplayName("No sube el precio de un producto cuyo margen ya llega al treinta por ciento")
    void noSubeElPrecioDeUnProductoCuyoMargenYaLlegaAlTreintaPorCiento() {
        dadoUnaYerbaConMargenDelTreintaPorCientoYVentas();

        cuandoSeEligenLosPasos();

        entoncesSoloSeProponePonerALaVista();
    }

    // --- dado ---

    private void dadoUnAlmacenConYerbaCacaoYAlfajorConVentasDeYerbaYAlfajor() {
        sistemas = List.of(datos(
                inventario(
                        producto(YERBA, "Yerba", 4, 3600, 2700),
                        producto(CACAO, "Cacao", 20, 2500, 1800),
                        producto(ALFAJOR, "Alfajor", 8, 1000, 400)),
                ventas(
                        venta(YERBA, 60, "2026-11-08"),
                        venta(ALFAJOR, 10, "2026-11-07"))));
    }

    private void dadoUnaZonaQueConoceLaYerbaEntre3500Y4300() {
        zona = zonaDe(8, new AgregadoZonaProducto(
                "yerbá", 6, 6, decimal("3500"), decimal("3900"), decimal("4300"), decimal("10")));
    }

    private void dadoUnSistemaSinInventarios() {
        sistemas = List.of(new DatosDeProyecto("Vacío", List.of(), List.of(), List.of()));
    }

    private void dadoProductosSinNombrePrecioOCostoValidos() {
        sistemas = List.of(datos(inventario(
                producto(id(11), null, 1, 100, 50),
                producto(id(12), "   ", 1, 100, 50),
                producto(id(13), "Sin precio", 1, null, 50),
                producto(id(14), "Precio cero", 1, 0, 50),
                producto(id(15), "Precio texto", 1, "100", 50),
                producto(id(16), "Sin costo", 1, 100, null),
                producto(id(17), "Costo cero", 1, 100, 0),
                producto(id(18), "Costo texto", 1, 100, "50")), ventas()));
    }

    private void dadoUnProductoSinStockNumerico() {
        sistemas = List.of(datos(inventario(producto(CACAO, "Cacao", null, 1000, 500)), ventas()));
    }

    private void dadoProductosBaratosYCarosConMargenJustoEInsuficiente() {
        sistemas = List.of(datos(inventario(
                producto(id(21), "Barato insuficiente", 100, 1000, 985),
                producto(id(22), "Barato justo", 2, 1000, 980),
                producto(id(23), "Caro insuficiente", 100, 2000, 1910),
                producto(id(24), "Caro justo", 3, 2000, 1900)), ventas()));
    }

    private void dadoDosProductosEmpatadosYUnaZonaConDosComercios() {
        RegistroId productoB = id(31);
        RegistroId productoA = id(32);
        sistemas = List.of(datos(
                inventario(
                        producto(productoB, "B producto", 2, 100, 80),
                        producto(productoA, "A producto", 2, 100, 80)),
                ventas(venta(productoB, 4, "2026-11-08"), venta(productoA, 4, "2026-11-08"))));
        zona = zonaDe(2, new AgregadoZonaProducto(
                "A producto", 2, 2, decimal("90"), decimal("100"), decimal("110"), decimal("3")));
    }

    private void dadoUnaYerbaConMargenDelTreintaPorCientoYVentas() {
        sistemas = List.of(datos(
                inventario(producto(YERBA, "Yerba", 4, 1000, 700)),
                ventas(venta(YERBA, 5, "2026-11-08"))));
    }

    // --- cuando ---

    private void cuandoSeEligenLosPasos() {
        pasos = PasosObjetivo.elegir(sistemas, metricasVentas, HOY, zona);
    }

    // --- entonces ---

    private void entoncesSeEligenLosTresPasosEnOrdenSinRepetirProductos() {
        assertThat(pasos).extracting(PasoObjetivo::tipo).containsExactly(
                TipoPasoObjetivo.SUBIR_PRECIO,
                TipoPasoObjetivo.MOVER_PARADO,
                TipoPasoObjetivo.PONER_A_LA_VISTA);
        assertThat(pasos).extracting(PasoObjetivo::nombre).containsExactly("Yerba", "Cacao", "Alfajor");
    }

    private void entoncesLaYerbaTraeElRangoAnonimoDeLaZonaYElCacaoNo() {
        assertThat(pasos.getFirst().vendidas30Dias()).isEqualByComparingTo("60");
        assertThat(pasos.getFirst().precioZona()).satisfies(precio -> {
            assertThat(precio.minimo()).isEqualByComparingTo("3500");
            assertThat(precio.promedio()).isEqualByComparingTo("3900");
            assertThat(precio.maximo()).isEqualByComparingTo("4300");
        });
        assertThat(pasos.get(1).precioZona()).isNull();
    }

    private void entoncesNoHayPasos() {
        assertThat(pasos).isEmpty();
    }

    private void entoncesSoloSeProponeMoverElCaroConMargenJusto() {
        assertThat(pasos).singleElement().satisfies(paso -> {
            assertThat(paso.tipo()).isEqualTo(TipoPasoObjetivo.MOVER_PARADO);
            assertThat(paso.nombre()).isEqualTo("Caro justo");
        });
    }

    private void entoncesSeDesempataPorNombreSinPublicarLaZona() {
        assertThat(pasos).extracting(PasoObjetivo::nombre).containsExactly("A producto", "B producto");
        assertThat(pasos).allSatisfy(paso -> assertThat(paso.precioZona()).isNull());
    }

    private void entoncesSoloSeProponePonerALaVista() {
        assertThat(pasos).singleElement().satisfies(paso ->
                assertThat(paso.tipo()).isEqualTo(TipoPasoObjetivo.PONER_A_LA_VISTA));
    }

    // --- datos ---

    private static DatosDeProyecto datos(BloqueInventario inventario, BloqueVentas ventas) {
        return new DatosDeProyecto("Almacén", List.of(inventario), List.of(ventas), List.of());
    }

    private static BloqueInventario inventario(Registro... productos) {
        return AvisoTestData.inventarioConPrecios(productos);
    }

    private static BloqueVentas ventas(Registro... ventas) {
        return AvisoTestData.ventas(ventas);
    }

    private static Registro producto(RegistroId id, Object nombre, Object stock, Object precio, Object costo) {
        return AvisoTestData.producto(id.valor(), nombre, stock, precio, costo);
    }

    private static Registro venta(RegistroId productoId, Object cantidad, String fecha) {
        return AvisoTestData.venta(productoId.toString(), cantidad, 100, fecha, null);
    }

    private static AnalisisZona zonaDe(long comercios, AgregadoZonaProducto... productos) {
        return new AnalisisZona("San Justo", comercios, List.of(productos));
    }

    private static RegistroId id(String valor) {
        return RegistroId.de(valor);
    }

    private static RegistroId id(int finalId) {
        return RegistroId.de(String.format("d0000000-0000-4000-8000-%012d", finalId));
    }

    private static BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}
