package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.objetivo.ComoVieneElNegocio.Semana;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.BloqueGastos;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
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

class ComoVieneElNegocioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final UUID YERBA = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID FIDEOS = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID GALLETITAS = UUID.fromString("d0000000-0000-4000-8000-000000000003");
    private static final CampoId CAMPO_FIADO_ID = CampoId.de("f0000000-0000-4000-8000-000000000016");
    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000004");
    private static final CampoId GASTO_FECHA_ID = CampoId.de("f0000000-0000-4000-8000-000000000022");
    private static final CampoId GASTO_TOTAL_ID = CampoId.de("f0000000-0000-4000-8000-000000000023");
    private static final CampoId GASTO_CATEGORIA_ID = CampoId.de("f0000000-0000-4000-8000-000000000024");

    @Test
    @DisplayName("Cuenta lo vendido, la mercadería, los gastos sin la categoría Mercadería, lo que queda y el fiado de los últimos 30 días")
    void armarShouldSummarizeTheLastThirtyDays() {
        DatosDeProyecto almacen = proyecto(
                List.of(venta(YERBA, 10, 3400, "2026-10-10", false),
                        venta(YERBA, 5, 3400, "2026-10-15", true),
                        venta(FIDEOS, 3, 1700, "2026-10-12", false),
                        venta(YERBA, 100, 3400, "2026-08-01", false)),
                List.of(gasto("2026-10-05", 8000, "Servicios"),
                        gasto("2026-10-06", 50000, "Mercadería"),
                        gasto("2026-08-01", 9999, "Alquiler")));

        ComoVieneElNegocio negocio = ComoVieneElNegocio.armar(List.of(almacen), HOY);

        thenImportes(negocio, 56100, 40500, 8000, 7600, 17000);
        assertThat(negocio.hayVentas()).isTrue();
    }

    @Test
    @DisplayName("Arma seis semanas, de la más antigua a Hoy, cada una con el resultado de los 30 días que terminan esa semana")
    void armarShouldBuildTheLastSixWeeks() {
        DatosDeProyecto almacen = proyecto(
                List.of(venta(YERBA, 10, 3400, "2026-10-10", false),
                        venta(YERBA, 5, 3400, "2026-10-15", true),
                        venta(FIDEOS, 3, 1700, "2026-10-12", false)),
                List.of(gasto("2026-10-05", 8000, "Servicios")));

        List<Semana> semanas = ComoVieneElNegocio.armar(List.of(almacen), HOY).semanas();

        assertThat(semanas).containsExactly(
                new Semana("Hace 5 sem.", 0),
                new Semana("Hace 4 sem.", 0),
                new Semana("Hace 3 sem.", 0),
                new Semana("Hace 2 sem.", -8000),
                new Semana("Hace 1 sem.", 4100),
                new Semana("Hoy", 7600));
    }

    @Test
    @DisplayName("Suma todos los sistemas del negocio, aunque alguno no tenga tabla de gastos")
    void armarShouldAddEverySystemOfTheBusiness() {
        DatosDeProyecto almacen = proyecto(List.of(venta(YERBA, 1, 3400, "2026-10-19", false)),
                List.of(gasto("2026-10-19", 400, "Servicios")));
        DatosDeProyecto kiosco = proyectoSinGastos(List.of(venta(GALLETITAS, 2, 1000, "2026-10-18", true)));

        ComoVieneElNegocio negocio = ComoVieneElNegocio.armar(List.of(almacen, kiosco), HOY);

        thenImportes(negocio, 5400, 2700, 400, 2300, 2000);
    }

    @Test
    @DisplayName("Redondea los importes a pesos enteros")
    void armarShouldRoundToWholePesos() {
        DatosDeProyecto almacen = proyectoSinGastos(List.of(venta(YERBA, 1, 3400.5, "2026-10-19", false)));

        ComoVieneElNegocio negocio = ComoVieneElNegocio.armar(List.of(almacen), HOY);

        thenImportes(negocio, 3401, 2700, 0, 701, 0);
    }

    @Test
    @DisplayName("Sin ventas en los últimos 30 días no inventa números: todo en cero y sin semanas")
    void armarShouldReturnNothingWithoutSales() {
        DatosDeProyecto almacen = proyecto(List.of(venta(YERBA, 3, 3400, "2026-08-01", false)),
                List.of(gasto("2026-10-10", 8000, "Servicios")));

        ComoVieneElNegocio negocio = ComoVieneElNegocio.armar(List.of(almacen), HOY);

        assertThat(negocio).isEqualTo(new ComoVieneElNegocio(false, 0, 0, 0, 0, 0, List.of()));
    }

    @Test
    @DisplayName("Un negocio sin sistemas no tiene ventas")
    void armarShouldReturnNothingWithoutSystems() {
        ComoVieneElNegocio negocio = ComoVieneElNegocio.armar(List.of(), HOY);

        assertThat(negocio.hayVentas()).isFalse();
        assertThat(negocio.semanas()).isEmpty();
    }

    // --- vendidas en 30 días ---

    @Test
    @DisplayName("Cuenta las unidades vendidas de cada producto en los últimos 30 días; sin ventas queda en cero")
    void vendidasEn30DiasShouldCountUnitsPerProduct() {
        DatosDeProyecto almacen = proyectoSinGastos(List.of(
                venta(YERBA, 10, 3400, "2026-10-10", false),
                venta(YERBA, 5, 3400, "2026-10-19", true),
                venta(YERBA, 7, 3400, "2026-09-19", false),
                venta(FIDEOS, 3, 1700, "2026-09-20", false),
                venta(UUID.randomUUID(), 4, 100, "2026-10-10", false)));

        Map<RegistroId, BigDecimal> vendidas = ComoVieneElNegocio.vendidasEn30Dias(almacen, HOY);

        assertThat(vendidas).containsOnlyKeys(RegistroId.de(YERBA), RegistroId.de(FIDEOS), RegistroId.de(GALLETITAS));
        assertThat(vendidas.get(RegistroId.de(YERBA))).isEqualByComparingTo("15");
        assertThat(vendidas.get(RegistroId.de(FIDEOS))).isEqualByComparingTo("3");
        assertThat(vendidas.get(RegistroId.de(GALLETITAS))).isEqualByComparingTo("0");
    }

    // --- assert ---
    private static void thenImportes(ComoVieneElNegocio negocio, long vendiste, long mercaderia, long gastos,
                                     long queda, long teDeben) {
        assertThat(negocio.vendiste()).isEqualTo(vendiste);
        assertThat(negocio.mercaderia()).isEqualTo(mercaderia);
        assertThat(negocio.gastos()).isEqualTo(gastos);
        assertThat(negocio.queda()).isEqualTo(queda);
        assertThat(negocio.teDeben()).isEqualTo(teDeben);
    }

    // --- helpers: proyectos ---
    private static DatosDeProyecto proyecto(List<Registro> ventas, List<Registro> gastos) {
        return new DatosDeProyecto("Almacén", List.of(inventario()), List.of(bloqueDeVentas(ventas)),
                List.of(gastosDe(gastos)));
    }

    private static DatosDeProyecto proyectoSinGastos(List<Registro> ventas) {
        return new DatosDeProyecto("Kiosco", List.of(inventario()), List.of(bloqueDeVentas(ventas)), List.of());
    }

    /** Yerba con costo, fideos sin costo y galletitas con costo cero: sólo la yerba suma mercadería. */
    private static BloqueInventario inventario() {
        return AvisoTestData.inventarioConPrecios(
                AvisoTestData.producto(YERBA, "Yerba Nobleza Gaucha", 40, 3400, 2700),
                AvisoTestData.producto(FIDEOS, "Fideos Luchetti", 10, 1700, null),
                AvisoTestData.producto(GALLETITAS, "Galletitas", 10, 1000, 0));
    }

    // --- helpers: ventas ---
    private static Registro venta(UUID producto, int cantidad, Number precio, String fecha, boolean fiado) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(AvisoTestData.CAMPO_PRODUCTO_ID.toString(), producto.toString());
        valores.put(AvisoTestData.CAMPO_VENTA_CANTIDAD_ID.toString(), cantidad);
        valores.put(AvisoTestData.CAMPO_VENTA_PRECIO_ID.toString(), precio);
        valores.put(AvisoTestData.CAMPO_FECHA_ID.toString(), fecha);
        valores.put(CAMPO_FIADO_ID.toString(), fiado);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID)
                .bloqueId(AvisoTestData.VENTAS_ID).valores(valores).build();
    }

    private static BloqueVentas bloqueDeVentas(List<Registro> ventas) {
        Bloque base = AvisoTestData.bloqueVentas();
        List<Campo> campos = new ArrayList<>(base.getCampos());
        campos.add(campo(CAMPO_FIADO_ID, AvisoTestData.VENTAS_ID, "credit"));
        Bloque bloque = Bloque.builder().id(base.getId()).negocioId(base.getNegocioId())
                .proyectoId(base.getProyectoId()).tipo(base.getTipo()).campos(campos).build();
        return new BloqueVentas(bloque, AvisoTestData.camposPorClave(bloque), ventas);
    }

    // --- helpers: gastos ---
    private static Registro gasto(String fecha, int total, String categoria) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(GASTO_FECHA_ID.toString(), fecha);
        valores.put(GASTO_TOTAL_ID.toString(), total);
        valores.put(GASTO_CATEGORIA_ID.toString(), categoria);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID).bloqueId(GASTOS_ID)
                .valores(valores).build();
    }

    private static BloqueGastos gastosDe(List<Registro> gastos) {
        Campo fecha = campo(GASTO_FECHA_ID, GASTOS_ID, "date");
        Campo total = campo(GASTO_TOTAL_ID, GASTOS_ID, "total");
        Campo categoria = campo(GASTO_CATEGORIA_ID, GASTOS_ID, "category");
        Bloque bloque = Bloque.builder().id(GASTOS_ID).negocioId(AvisoTestData.NEGOCIO_ID)
                .proyectoId(AvisoTestData.PROYECTO_ID).tipo(TipoBloque.GASTOS)
                .campos(List.of(fecha, total, categoria)).build();
        return new BloqueGastos(bloque, Map.of("date", fecha, "total", total, "category", categoria), gastos);
    }

    private static Campo campo(CampoId id, BloqueId bloqueId, String clave) {
        return Campo.builder().id(id).negocioId(AvisoTestData.NEGOCIO_ID).bloqueId(bloqueId).clave(clave).build();
    }
}
