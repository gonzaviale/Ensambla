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

    @Test
    @DisplayName("Elige los tres pasos en orden, sin repetir productos y con el rango anónimo de la zona")
    void elegirShouldReturnOrderedDistinctStepsWithPrivateZoneRange() {
        DatosDeProyecto datos = datos(
                inventario(
                        producto(YERBA, "Yerba", 4, 3600, 2700),
                        producto(CACAO, "Cacao", 20, 2500, 1800),
                        producto(ALFAJOR, "Alfajor", 8, 1000, 400)),
                ventas(
                        venta(YERBA, 60, "2026-11-08"),
                        venta(ALFAJOR, 10, "2026-11-07")));
        AnalisisZona zona = zona(8, new AgregadoZonaProducto(
                "yerbá", 6, 6, decimal("3500"), decimal("3900"), decimal("4300"), decimal("10")));

        List<PasoObjetivo> pasos = elegir(List.of(datos), zona);

        assertThat(pasos).extracting(PasoObjetivo::tipo).containsExactly(
                TipoPasoObjetivo.SUBIR_PRECIO,
                TipoPasoObjetivo.MOVER_PARADO,
                TipoPasoObjetivo.PONER_A_LA_VISTA);
        assertThat(pasos).extracting(PasoObjetivo::nombre).containsExactly("Yerba", "Cacao", "Alfajor");
        assertThat(pasos.getFirst().vendidas30Dias()).isEqualByComparingTo("60");
        assertThat(pasos.getFirst().precioZona()).satisfies(precio -> {
            assertThat(precio.minimo()).isEqualByComparingTo("3500");
            assertThat(precio.promedio()).isEqualByComparingTo("3900");
            assertThat(precio.maximo()).isEqualByComparingTo("4300");
        });
        assertThat(pasos.get(1).precioZona()).isNull();
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando no hay inventarios")
    void elegirShouldReturnEmptyListWithoutInventories() {
        List<PasoObjetivo> pasos = elegir(List.of(new DatosDeProyecto(
                "Vacío", List.of(), List.of(), List.of())), AnalisisZona.sinZona());

        assertThat(pasos).isEmpty();
    }

    @Test
    @DisplayName("Ignora productos sin nombre, precio o costo numérico y positivo")
    void elegirShouldIgnoreProductsWithoutRequiredValues() {
        DatosDeProyecto datos = datos(inventario(
                producto(id(11), null, 1, 100, 50),
                producto(id(12), "   ", 1, 100, 50),
                producto(id(13), "Sin precio", 1, null, 50),
                producto(id(14), "Precio cero", 1, 0, 50),
                producto(id(15), "Precio texto", 1, "100", 50),
                producto(id(16), "Sin costo", 1, 100, null),
                producto(id(17), "Costo cero", 1, 100, 0),
                producto(id(18), "Costo texto", 1, 100, "50")), ventas());

        List<PasoObjetivo> pasos = elegir(List.of(datos), AnalisisZona.sinZona());

        assertThat(pasos).isEmpty();
    }

    @Test
    @DisplayName("Un producto sin stock numérico no se propone como mercadería parada")
    void elegirShouldTreatMissingStockAsZero() {
        DatosDeProyecto datos = datos(inventario(
                producto(CACAO, "Cacao", null, 1000, 500)), ventas());

        List<PasoObjetivo> pasos = elegir(List.of(datos), AnalisisZona.sinZona());

        assertThat(pasos).isEmpty();
    }

    @Test
    @DisplayName("Exige dos pasos de margen para rebajar productos baratos y caros")
    void elegirShouldRequireTwoPriceStepsToMoveStoppedProducts() {
        DatosDeProyecto datos = datos(inventario(
                producto(id(21), "Barato insuficiente", 100, 1000, 985),
                producto(id(22), "Barato justo", 2, 1000, 980),
                producto(id(23), "Caro insuficiente", 100, 2000, 1910),
                producto(id(24), "Caro justo", 3, 2000, 1900)), ventas());

        List<PasoObjetivo> pasos = elegir(List.of(datos), AnalisisZona.sinZona());

        assertThat(pasos).singleElement().satisfies(paso -> {
            assertThat(paso.tipo()).isEqualTo(TipoPasoObjetivo.MOVER_PARADO);
            assertThat(paso.nombre()).isEqualTo("Caro justo");
        });
    }

    @Test
    @DisplayName("Resuelve empates por nombre y no publica zona con menos de tres comercios")
    void elegirShouldBreakTiesByNameAndHideInsufficientZoneData() {
        RegistroId productoB = id(31);
        RegistroId productoA = id(32);
        DatosDeProyecto datos = datos(
                inventario(
                        producto(productoB, "B producto", 2, 100, 80),
                        producto(productoA, "A producto", 2, 100, 80)),
                ventas(venta(productoB, 4, "2026-11-08"), venta(productoA, 4, "2026-11-08")));
        AnalisisZona zona = zona(2, new AgregadoZonaProducto(
                "A producto", 2, 2, decimal("90"), decimal("100"), decimal("110"), decimal("3")));

        List<PasoObjetivo> pasos = elegir(List.of(datos), zona);

        assertThat(pasos).extracting(PasoObjetivo::nombre).containsExactly("A producto", "B producto");
        assertThat(pasos).allSatisfy(paso -> assertThat(paso.precioZona()).isNull());
    }

    @Test
    @DisplayName("No sube el precio de un producto cuyo margen ya llega al treinta por ciento")
    void elegirShouldNotRaisePriceAtThirtyPercentMargin() {
        DatosDeProyecto datos = datos(
                inventario(producto(YERBA, "Yerba", 4, 1000, 700)),
                ventas(venta(YERBA, 5, "2026-11-08")));

        List<PasoObjetivo> pasos = elegir(List.of(datos), AnalisisZona.sinZona());

        assertThat(pasos).singleElement().satisfies(paso ->
                assertThat(paso.tipo()).isEqualTo(TipoPasoObjetivo.PONER_A_LA_VISTA));
    }

    private List<PasoObjetivo> elegir(List<DatosDeProyecto> datos, AnalisisZona zona) {
        return PasosObjetivo.elegir(datos, metricasVentas, HOY, zona);
    }

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

    private static AnalisisZona zona(long comercios, AgregadoZonaProducto... productos) {
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
