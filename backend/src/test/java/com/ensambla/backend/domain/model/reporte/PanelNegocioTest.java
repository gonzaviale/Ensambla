package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.PanelNegocio.SemanaDeCaja;
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

class PanelNegocioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final PeriodoResumen PERIODO = PeriodoResumen.armar(null, null, HOY);
    private static final UUID YERBA = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID FIDEOS = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID ACEITE = UUID.fromString("d0000000-0000-4000-8000-000000000004");
    private static final UUID DESCONOCIDO = UUID.fromString("d0000000-0000-4000-8000-000000000009");
    private static final CampoId CAMPO_FIADO_ID = CampoId.de("f0000000-0000-4000-8000-000000000016");
    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000004");
    private static final CampoId GASTO_FECHA_ID = CampoId.de("f0000000-0000-4000-8000-000000000022");
    private static final CampoId GASTO_TOTAL_ID = CampoId.de("f0000000-0000-4000-8000-000000000023");
    private static final CampoId GASTO_CATEGORIA_ID = CampoId.de("f0000000-0000-4000-8000-000000000024");
    private static final BloqueId ENTRADAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId ENTRADA_PRODUCTO_ID = CampoId.de("f0000000-0000-4000-8000-000000000041");
    private static final CampoId ENTRADA_CANTIDAD_ID = CampoId.de("f0000000-0000-4000-8000-000000000042");
    private static final CampoId ENTRADA_FECHA_ID = CampoId.de("f0000000-0000-4000-8000-000000000043");

    // --- métricas ---

    @Test
    @DisplayName("Las métricas de ventas, gastos y fiado coinciden con las del resumen del mismo período")
    void armarShouldMatchTheSummaryOfTheSamePeriod() {
        DatosDeProyecto datos = datosCompletos();

        PanelNegocio panel = PanelNegocio.armar(datos, PERIODO, HOY);

        ResumenNegocio resumen = ResumenNegocio.armar(
                "", datos, AnalisisZona.sinZona(), List.of(), PERIODO, HOY);
        assertThat(panel.vacio()).isFalse();
        assertThat(panel.periodo()).isEqualTo(PERIODO);
        assertThat(panel.metricas().vendiste()).isEqualByComparingTo(resumen.ventas().ingresos());
        assertThat(panel.metricas().vendiste()).isEqualByComparingTo("54400");
        assertThat(panel.metricas().gastos()).isEqualByComparingTo(resumen.gastos().total());
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo(resumen.clientes().fiado());
        assertThat(panel.metricas().variacion()).isEqualByComparingTo(resumen.ventas().variacion());
    }

    @Test
    @DisplayName("Calcula lo comprado, lo que queda, el margen y el ticket promedio")
    void armarShouldComputePurchasesLeftoverMarginAndAverageTicket() {
        PanelNegocio panel = PanelNegocio.armar(datosCompletos(), PERIODO, HOY);

        PanelNegocio.Metricas metricas = panel.metricas();
        assertThat(metricas.mercaderia()).isEqualByComparingTo("13500");
        assertThat(metricas.gastos()).isEqualByComparingTo("6500");
        assertThat(metricas.teDeben()).isEqualByComparingTo("17000");
        assertThat(metricas.queda()).isEqualByComparingTo("17400");
        assertThat(metricas.margen()).isEqualByComparingTo("20.6");
        assertThat(metricas.ticketPromedio()).isEqualByComparingTo("18133.33");
        assertThat(metricas.variacion()).isEqualByComparingTo("220");
    }

    @Test
    @DisplayName("Sin tabla de gastos ni de entradas esos datos quedan sin informar, no en cero")
    void armarShouldLeaveExpensesAndPurchasesUnknownWithoutTheirTables() {
        DatosDeProyecto datos = new DatosDeProyecto("Almacén",
                List.of(AvisoTestData.inventarioConPrecios(yerba())),
                List.of(bloqueDeVentas(List.of(venta(YERBA, 10, 3400, "2026-09-22", false)))), List.of());

        PanelNegocio panel = PanelNegocio.armar(datos, PERIODO, HOY);

        assertThat(panel.metricas().gastos()).isNull();
        assertThat(panel.metricas().mercaderia()).isNull();
        assertThat(panel.metricas().queda()).isEqualByComparingTo("34000");
        assertThat(panel.metricas().variacion()).isNull();
    }

    @Test
    @DisplayName("Un gasto sin importe, sin fecha o de otro período no suma")
    void armarShouldIgnoreExpensesThatCannotBeCounted() {
        List<Registro> gastos = List.of(
                gasto("2026-09-23", 5000), gasto("2026-09-23", null), gasto("2026-08-01", 9999), gasto(null, 700));

        PanelNegocio panel = PanelNegocio.armar(datosConGastos(gastos), PERIODO, HOY);

        assertThat(panel.metricas().gastos()).isEqualByComparingTo("5000");
        assertThat(panel.flujoDeCaja().getFirst().salidas()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("Sin costo en los productos vendidos no hay margen")
    void armarShouldNotComputeMarginWithoutCosts() {
        DatosDeProyecto datos = new DatosDeProyecto("Almacén",
                List.of(AvisoTestData.inventarioConPrecios(fideos())),
                List.of(bloqueDeVentas(List.of(venta(FIDEOS, 2, 1700, "2026-10-12", false)))), List.of());

        PanelNegocio panel = PanelNegocio.armar(datos, PERIODO, HOY);

        assertThat(panel.metricas().margen()).isNull();
        assertThat(panel.metricas().ticketPromedio()).isEqualByComparingTo("3400");
    }

    // --- flujo de caja ---

    @Test
    @DisplayName("Reparte entradas y salidas por semana desde el primer día, con el saldo acumulado")
    void armarShouldSplitTheCashFlowByWeek() {
        PanelNegocio panel = PanelNegocio.armar(datosCompletos(), PERIODO, HOY);

        assertThat(panel.flujoDeCaja()).hasSize(5);
        thenSemana(panel.flujoDeCaja().get(0), "2026-09-20", "2026-09-26", "34000", "18500", "15500", "15500");
        thenSemana(panel.flujoDeCaja().get(1), "2026-09-27", "2026-10-03", "0", "0", "0", "15500");
        thenSemana(panel.flujoDeCaja().get(2), "2026-10-04", "2026-10-10", "0", "0", "0", "15500");
        thenSemana(panel.flujoDeCaja().get(3), "2026-10-11", "2026-10-17", "3400", "1500", "1900", "17400");
        thenSemana(panel.flujoDeCaja().get(4), "2026-10-18", "2026-10-19", "0", "0", "0", "17400");
    }

    @Test
    @DisplayName("Lo vendido a fiado no es una entrada hasta cobrarse: se informa aparte")
    void armarShouldKeepCreditSalesOutOfTheCashInflow() {
        PanelNegocio panel = PanelNegocio.armar(datosCompletos(), PERIODO, HOY);

        BigDecimal entradas = panel.flujoDeCaja().stream()
                .map(SemanaDeCaja::entradas)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(entradas).isEqualByComparingTo("37400");
        assertThat(panel.metricas().vendiste()).isEqualByComparingTo("54400");
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo("17000");
    }

    @Test
    @DisplayName("Un período de una semana exacta tiene una sola semana")
    void armarShouldReturnASingleWeekForSevenDays() {
        PeriodoResumen semana = PeriodoResumen.ultimosDias(7, HOY);

        PanelNegocio panel = PanelNegocio.armar(datosCompletos(), semana, HOY);

        assertThat(panel.flujoDeCaja()).hasSize(1);
        thenSemana(panel.flujoDeCaja().getFirst(), "2026-10-13", "2026-10-19", "0", "0", "0", "0");
    }

    @Test
    @DisplayName("Una compra sin producto, cantidad, fecha o costo conocido no cuenta como mercadería")
    void armarShouldIgnorePurchasesItCannotValue() {
        List<Registro> entradas = List.of(
                entrada(YERBA.toString(), 5, "2026-09-24"),
                entrada(DESCONOCIDO.toString(), 5, "2026-09-24"),
                entrada(FIDEOS.toString(), 5, "2026-09-24"),
                entrada(ACEITE.toString(), 5, "2026-09-24"),
                entrada(YERBA.toString(), null, "2026-09-24"),
                entrada(YERBA.toString(), 0, "2026-09-24"),
                entrada(YERBA.toString(), 1, "2026-08-01"),
                entrada(YERBA.toString(), 1, "ayer"),
                entrada(YERBA.toString(), 1, null),
                entrada("no-es-un-id", 1, "2026-09-24"),
                entrada(null, 1, "2026-09-24"));

        PanelNegocio panel = PanelNegocio.armar(datosConEntradas(entradas), PERIODO, HOY);

        assertThat(panel.metricas().mercaderia()).isEqualByComparingTo("13500");
    }

    // --- mercadería anotada como gasto ---

    @Test
    @DisplayName("La categoría Mercadería no es un gasto ni se suma a las entradas: sería contar la compra dos veces")
    void armarShouldNotCountMerchandiseExpensesTwice() {
        List<Registro> gastos = List.of(
                gasto("2026-09-23", 5000),
                gasto("2026-10-12", 1500),
                gasto("2026-10-02", 700, "Servicios"),
                gasto("2026-10-01", 9000, "Mercadería"));

        PanelNegocio panel = PanelNegocio.armar(datosConGastosYEntradas(gastos), PERIODO, HOY);

        PanelNegocio.Metricas metricas = panel.metricas();
        assertThat(metricas.gastos()).isEqualByComparingTo("7200");
        assertThat(metricas.mercaderia()).isEqualByComparingTo("13500");
        assertThat(metricas.queda()).isEqualByComparingTo("16700");
    }

    @Test
    @DisplayName("Sin tabla de entradas, la mercadería anotada como gasto es la mercadería y no un gasto más")
    void armarShouldTakePurchasesFromExpensesWithoutAnEntriesTable() {
        List<Registro> gastos = List.of(
                gasto("2026-09-23", 5000),
                gasto("2026-09-24", 9000, " mercaderia "),
                gasto("2026-08-01", 700, "Mercadería"));
        DatosDeProyecto datos = new DatosDeProyecto("Almacén",
                List.of(AvisoTestData.inventarioConPrecios(yerba())),
                List.of(bloqueDeVentas(List.of(venta(YERBA, 10, 3400, "2026-09-22", false)))),
                List.of(gastosDe(gastos)));

        PanelNegocio panel = PanelNegocio.armar(datos, PERIODO, HOY);

        assertThat(panel.metricas().gastos()).isEqualByComparingTo("5000");
        assertThat(panel.metricas().mercaderia()).isEqualByComparingTo("9000");
        assertThat(panel.flujoDeCaja().getFirst().salidas()).isEqualByComparingTo("14000");
        assertThat(panel.metricas().queda()).isEqualByComparingTo("20000");
    }

    @Test
    @DisplayName("Ventas sin precio cuentan como venta pero no hay ticket ni margen que calcular")
    void armarShouldNotComputeTicketOrMarginWithoutPrices() {
        DatosDeProyecto datos = new DatosDeProyecto("Almacén",
                List.of(AvisoTestData.inventarioConPrecios(yerba())),
                List.of(bloqueDeVentas(List.of(venta(YERBA, 3, null, "2026-10-12", false)))), List.of());

        PanelNegocio panel = PanelNegocio.armar(datos, PERIODO, HOY);

        assertThat(panel.vacio()).isFalse();
        assertThat(panel.metricas().vendiste()).isEqualByComparingTo("0");
        assertThat(panel.metricas().ticketPromedio()).isNull();
        assertThat(panel.metricas().margen()).isNull();
    }

    // --- sin datos ---

    @Test
    @DisplayName("Sin ventas en el período el panel está vacío: sin métricas ni semanas, no ceros")
    void armarShouldBeEmptyWithoutSalesInThePeriod() {
        DatosDeProyecto datos = new DatosDeProyecto("Almacén",
                List.of(AvisoTestData.inventarioConPrecios(yerba())),
                List.of(bloqueDeVentas(List.of(venta(YERBA, 10, 3400, "2026-08-01", false)))), List.of());

        PanelNegocio panel = PanelNegocio.armar(datos, PERIODO, HOY);

        assertThat(panel.vacio()).isTrue();
        assertThat(panel.metricas()).isNull();
        assertThat(panel.flujoDeCaja()).isEmpty();
        assertThat(panel.periodo()).isEqualTo(PERIODO);
    }

    @Test
    @DisplayName("Sin tabla de ventas el panel está vacío")
    void armarShouldBeEmptyWithoutASalesTable() {
        DatosDeProyecto datos = new DatosDeProyecto("Almacén", List.of(), List.of(), List.of());

        assertThat(PanelNegocio.armar(datos, PERIODO, HOY).vacio()).isTrue();
    }

    // --- datos de prueba ---

    private static DatosDeProyecto datosCompletos() {
        List<Registro> ventas = List.of(
                venta(YERBA, 10, 3400, "2026-09-22", false),
                venta(YERBA, 5, 3400, "2026-10-05", true),
                venta(FIDEOS, 2, 1700, "2026-10-12", false),
                venta(YERBA, 2, null, "2026-10-13", false),
                venta(YERBA, 5, 3400, "2026-09-10", false),
                venta(YERBA, 100, 3400, "2026-08-01", false));
        List<Registro> gastos = List.of(
                gasto("2026-09-23", 5000), gasto("2026-10-12", 1500), gasto("2026-08-01", 9999));
        List<Registro> entradas = List.of(
                entrada(YERBA.toString(), 5, "2026-09-24"), entrada(YERBA.toString(), 5, "2026-08-01"));
        return new DatosDeProyecto("Almacén",
                List.of(AvisoTestData.inventarioConPrecios(yerba(), fideos(), aceite())),
                List.of(bloqueDeVentas(ventas)), List.of(gastosDe(gastos)), Map.of(),
                List.of(entradasDe(entradas)));
    }

    private static DatosDeProyecto datosConGastos(List<Registro> gastos) {
        return new DatosDeProyecto("Almacén",
                List.of(AvisoTestData.inventarioConPrecios(yerba())),
                List.of(bloqueDeVentas(List.of(venta(YERBA, 10, 3400, "2026-09-22", false)))),
                List.of(gastosDe(gastos)));
    }

    private static DatosDeProyecto datosConGastosYEntradas(List<Registro> gastos) {
        return new DatosDeProyecto("Almacén",
                List.of(AvisoTestData.inventarioConPrecios(yerba(), fideos(), aceite())),
                List.of(bloqueDeVentas(List.of(
                        venta(YERBA, 10, 3400, "2026-09-22", false), venta(FIDEOS, 2, 1700, "2026-10-12", false)))),
                List.of(gastosDe(gastos)), Map.of(),
                List.of(entradasDe(List.of(entrada(YERBA.toString(), 5, "2026-09-24")))));
    }

    private static DatosDeProyecto datosConEntradas(List<Registro> entradas) {
        return new DatosDeProyecto("Almacén",
                List.of(AvisoTestData.inventarioConPrecios(yerba(), fideos(), aceite())),
                List.of(bloqueDeVentas(List.of(venta(YERBA, 10, 3400, "2026-09-22", false)))), List.of(), Map.of(),
                List.of(entradasDe(entradas)));
    }

    private static Registro yerba() {
        return producto(YERBA, "Yerba Nobleza Gaucha", 40, 3400, 2700);
    }

    private static Registro fideos() {
        return producto(FIDEOS, "Fideos Luchetti", 0, 1700, null);
    }

    /** Su costo es cero: tampoco sirve para valuar una compra. */
    private static Registro aceite() {
        return producto(ACEITE, "Aceite Cocinero", 3, 2800, 0);
    }

    private static Registro producto(UUID id, String nombre, Object cantidad, Object precio, Object costo) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, AvisoTestData.CAMPO_NOMBRE_ID, nombre);
        poner(valores, AvisoTestData.CAMPO_CANTIDAD_ID, cantidad);
        poner(valores, AvisoTestData.CAMPO_PRECIO_ID, precio);
        poner(valores, AvisoTestData.CAMPO_COSTO_ID, costo);
        return Registro.builder().id(RegistroId.de(id)).negocioId(AvisoTestData.NEGOCIO_ID)
                .bloqueId(AvisoTestData.INVENTARIO_ID).valores(valores).build();
    }

    private static Registro venta(UUID producto, int cantidad, Integer precio, String fecha, boolean fiado) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, AvisoTestData.CAMPO_PRODUCTO_ID, producto.toString());
        poner(valores, AvisoTestData.CAMPO_VENTA_CANTIDAD_ID, cantidad);
        poner(valores, AvisoTestData.CAMPO_VENTA_PRECIO_ID, precio);
        poner(valores, AvisoTestData.CAMPO_FECHA_ID, fecha);
        poner(valores, CAMPO_FIADO_ID, fiado);
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

    private static Registro gasto(String fecha, Integer total) {
        return gasto(fecha, total, null);
    }

    private static Registro gasto(String fecha, Integer total, String categoria) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, GASTO_FECHA_ID, fecha);
        poner(valores, GASTO_TOTAL_ID, total);
        poner(valores, GASTO_CATEGORIA_ID, categoria);
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

    private static Registro entrada(String producto, Integer cantidad, String fecha) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, ENTRADA_PRODUCTO_ID, producto);
        poner(valores, ENTRADA_CANTIDAD_ID, cantidad);
        poner(valores, ENTRADA_FECHA_ID, fecha);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID).bloqueId(ENTRADAS_ID)
                .valores(valores).build();
    }

    private static BloqueEntradas entradasDe(List<Registro> entradas) {
        Campo producto = campo(ENTRADA_PRODUCTO_ID, ENTRADAS_ID, "product");
        Campo cantidad = campo(ENTRADA_CANTIDAD_ID, ENTRADAS_ID, "quantity");
        Campo fecha = campo(ENTRADA_FECHA_ID, ENTRADAS_ID, "date");
        Bloque bloque = Bloque.builder().id(ENTRADAS_ID).negocioId(AvisoTestData.NEGOCIO_ID)
                .proyectoId(AvisoTestData.PROYECTO_ID).tipo(TipoBloque.ENTRADAS)
                .campos(List.of(producto, cantidad, fecha)).build();
        return new BloqueEntradas(bloque, Map.of("product", producto, "quantity", cantidad, "date", fecha), entradas);
    }

    private static Campo campo(CampoId id, BloqueId bloqueId, String clave) {
        return Campo.builder().id(id).negocioId(AvisoTestData.NEGOCIO_ID).bloqueId(bloqueId).clave(clave).build();
    }

    private static void poner(Map<String, Object> valores, CampoId campoId, Object valor) {
        if (valor != null) {
            valores.put(campoId.toString(), valor);
        }
    }

    // --- verificaciones ---

    private static void thenSemana(SemanaDeCaja semana, String desde, String hasta, String entradas, String salidas,
                                   String saldo, String acumulado) {
        assertThat(semana.desde()).isEqualTo(LocalDate.parse(desde));
        assertThat(semana.hasta()).isEqualTo(LocalDate.parse(hasta));
        assertThat(semana.entradas()).isEqualByComparingTo(entradas);
        assertThat(semana.salidas()).isEqualByComparingTo(salidas);
        assertThat(semana.saldo()).isEqualByComparingTo(saldo);
        assertThat(semana.saldoAcumulado()).isEqualByComparingTo(acumulado);
    }
}
