package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica.Fila;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.reporte.BloqueGastos;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.CATEGORIA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.CLIENTE;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.DIA_DE_LA_SEMANA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.NINGUNA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.PRODUCTO;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.CONTEO;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.PROMEDIO;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.SUMA;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.UNIDADES;
import static com.ensambla.backend.domain.model.metrica.FuenteMetrica.GASTOS;
import static com.ensambla.backend.domain.model.metrica.FuenteMetrica.INVENTARIO;
import static com.ensambla.backend.domain.model.metrica.FuenteMetrica.VENTAS;
import static com.ensambla.backend.domain.model.metrica.PeriodoMetrica.MES;
import static com.ensambla.backend.domain.model.metrica.PeriodoMetrica.SEMANA;
import static com.ensambla.backend.domain.model.metrica.PeriodoMetrica.TODO;
import static com.ensambla.backend.support.MetricaTestData.HOY;
import static com.ensambla.backend.support.MetricaTestData.LECHE_ID;
import static com.ensambla.backend.support.MetricaTestData.PAN_ID;
import static com.ensambla.backend.support.MetricaTestData.SAL_ID;
import static com.ensambla.backend.support.MetricaTestData.datos;
import static com.ensambla.backend.support.MetricaTestData.definicion;
import static com.ensambla.backend.support.MetricaTestData.gasto;
import static com.ensambla.backend.support.MetricaTestData.gastos;
import static com.ensambla.backend.support.MetricaTestData.gastosSinImporte;
import static com.ensambla.backend.support.MetricaTestData.inventario;
import static com.ensambla.backend.support.MetricaTestData.inventarioSinCantidad;
import static com.ensambla.backend.support.MetricaTestData.venta;
import static com.ensambla.backend.support.MetricaTestData.ventas;
import static org.assertj.core.api.Assertions.assertThat;

class MotorMetricasTest {

    private final MotorMetricas motor = new MotorMetricas();

    @Test
    @DisplayName("Suma lo vendido del mes y cuenta aparte las ventas sin precio")
    void calcularShouldSumSalesOfTheMonthAndCountSalesWithoutPrice() {
        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, NINGUNA, MES), cuaderno());

        assertThat(resultado.calculable()).isTrue();
        assertThat(resultado.formato()).isEqualTo(FormatoMetrica.MONEDA);
        assertThat(resultado.valor()).isEqualByComparingTo("1000");
        assertThat(resultado.filas()).isEmpty();
        assertThat(resultado.deDondeSale().filasUsadas()).isEqualTo(3);
        assertThat(resultado.deDondeSale().filasSinDato()).isEqualTo(1);
    }

    @Test
    @DisplayName("Mira sólo los últimos 7 días en una semana y dice desde y hasta cuándo")
    void calcularShouldLookAtTheLastSevenDays() {
        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, NINGUNA, SEMANA), cuaderno());

        assertThat(resultado.valor()).isEqualByComparingTo("550");
        assertThat(resultado.deDondeSale().desde()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(resultado.deDondeSale().hasta()).isEqualTo(HOY);
    }

    @Test
    @DisplayName("Mira sólo lo de hoy cuando el período es hoy")
    void calcularShouldLookOnlyAtToday() {
        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, NINGUNA, PeriodoMetrica.HOY), cuaderno());

        assertThat(resultado.valor()).isEqualByComparingTo("300");
    }

    @Test
    @DisplayName("Mira todo lo cargado sin fechas cuando el período es todo")
    void calcularShouldLookAtEverythingWhenPeriodIsAll() {
        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, NINGUNA, TODO), cuaderno());

        assertThat(resultado.valor()).isEqualByComparingTo("1150");
        assertThat(resultado.deDondeSale().desde()).isNull();
        assertThat(resultado.deDondeSale().hasta()).isNull();
    }

    @Test
    @DisplayName("Promedia el importe de las ventas que tienen precio")
    void calcularShouldAverageSalesWithPrice() {
        ResultadoMetrica resultado = calcular(definicion(VENTAS, PROMEDIO, NINGUNA, MES), cuaderno());

        assertThat(resultado.valor()).isEqualByComparingTo("333.33");
    }

    @Test
    @DisplayName("Cuenta todas las ventas, también las que no tienen precio")
    void calcularShouldCountAllSales() {
        ResultadoMetrica resultado = calcular(definicion(VENTAS, CONTEO, NINGUNA, MES), cuaderno());

        assertThat(resultado.formato()).isEqualTo(FormatoMetrica.NUMERO);
        assertThat(resultado.valor()).isEqualByComparingTo("4");
        assertThat(resultado.deDondeSale().filasSinDato()).isZero();
    }

    @Test
    @DisplayName("Suma las unidades vendidas")
    void calcularShouldSumUnits() {
        ResultadoMetrica resultado = calcular(definicion(VENTAS, UNIDADES, NINGUNA, MES), cuaderno());

        assertThat(resultado.valor()).isEqualByComparingTo("7");
    }

    @Test
    @DisplayName("Reparte por producto de mayor a menor con el nombre del inventario")
    void calcularShouldSplitByProductWithInventoryNames() {
        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, PRODUCTO, MES), cuaderno());

        assertThat(resultado.filas()).extracting(Fila::etiqueta).containsExactly("Pan", "Leche");
        assertThat(resultado.filas().get(0).valor()).isEqualByComparingTo("750");
        assertThat(resultado.filas().get(1).valor()).isEqualByComparingTo("250");
    }

    @Test
    @DisplayName("Reparte por cliente y pone 'Sin cliente' a las ventas sin cliente")
    void calcularShouldSplitByCustomer() {
        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, CLIENTE, MES), cuaderno());

        assertThat(resultado.filas()).extracting(Fila::etiqueta).containsExactly("Sin cliente", "Ana", "Beto");
    }

    @Test
    @DisplayName("Muestra el nombre del cliente cuando la venta guarda la conexión a la tabla de clientes")
    void calcularShouldShowCustomerNameFromClientsTable() {
        DatosDeProyecto datos = datos(List.of(inventario()), List.of(ventas(
                venta(PAN_ID, 1, 100, "2026-11-10", "id-ana", false))), List.of(), Map.of("id-ana", "Ana García"));

        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, CLIENTE, MES), datos);

        assertThat(resultado.filas()).extracting(Fila::etiqueta).containsExactly("Ana García");
    }

    @Test
    @DisplayName("Reparte por día de la semana en orden de lunes a domingo")
    void calcularShouldSplitByWeekdayInOrder() {
        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, DIA_DE_LA_SEMANA, MES), cuaderno());

        assertThat(resultado.filas()).extracting(Fila::etiqueta).containsExactly("Martes", "Miércoles");
        assertThat(resultado.filas().get(1).valor()).isEqualByComparingTo("750");
    }

    @Test
    @DisplayName("Con 'lo que te deben' suma sólo las ventas fiadas")
    void calcularShouldSumOnlyCreditSales() {
        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, CLIENTE, TODO, true), cuaderno());

        assertThat(resultado.valor()).isEqualByComparingTo("450");
        assertThat(resultado.filas()).singleElement().satisfies(fila -> {
            assertThat(fila.etiqueta()).isEqualTo("Ana");
            assertThat(fila.valor()).isEqualByComparingTo("450");
        });
    }

    @Test
    @DisplayName("Muestra como mucho 10 partes del reparto")
    void calcularShouldLimitTheSplit() {
        List<Registro> filas = new ArrayList<>();
        for (int numero = 1; numero <= 12; numero++) {
            filas.add(venta(PAN_ID, 1, numero * 10, "2026-11-10", "cliente " + numero, false));
        }
        DatosDeProyecto datos = datos(
                List.of(inventario()), List.of(ventas(filas.toArray(Registro[]::new))), List.of());

        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, CLIENTE, MES), datos);

        assertThat(resultado.filas()).hasSize(MotorMetricas.MAXIMO_DE_FILAS);
        assertThat(resultado.filas().get(0).etiqueta()).isEqualTo("cliente 12");
    }

    @Test
    @DisplayName("Ignora las ventas que no se pueden leer")
    void calcularShouldIgnoreUnreadableSalesAndNameMissingProducts() {
        DatosDeProyecto datos = datos(List.of(inventario()), List.of(ventas(
                venta(null, 1, 100, "2026-11-10", null, false),
                venta(SAL_ID, 1, 100, "2026-11-10", null, null))), List.of());

        ResultadoMetrica resultado = calcular(definicion(VENTAS, CONTEO, PRODUCTO, MES), datos);

        assertThat(resultado.valor()).isEqualByComparingTo("1");
        assertThat(resultado.filas()).extracting(Fila::etiqueta).containsExactly("Sal");
    }

    @Test
    @DisplayName("Suma los gastos del mes, ignora el que no tiene importe y lo cuenta aparte")
    void calcularShouldSumExpensesOfTheMonth() {
        ResultadoMetrica resultado = calcular(definicion(GASTOS, SUMA, NINGUNA, MES), cuaderno());

        assertThat(resultado.valor()).isEqualByComparingTo("1200");
        assertThat(resultado.deDondeSale().filasUsadas()).isEqualTo(2);
        assertThat(resultado.deDondeSale().filasSinDato()).isEqualTo(2);
    }

    @Test
    @DisplayName("Reparte los gastos por categoría y pone 'Sin categoría' al que no la tiene")
    void calcularShouldSplitExpensesByCategory() {
        ResultadoMetrica resultado = calcular(definicion(GASTOS, SUMA, CATEGORIA, TODO), cuaderno());

        assertThat(resultado.filas()).extracting(Fila::etiqueta)
                .containsExactly("Alquiler", "Sin categoría", "Luz");
        assertThat(resultado.filas().get(2).valor()).isEqualByComparingTo("75");
    }

    @Test
    @DisplayName("Reparte los gastos por día y deja al final los que no tienen fecha")
    void calcularShouldSplitExpensesByWeekdayAndPutUndatedLast() {
        ResultadoMetrica resultado = calcular(definicion(GASTOS, SUMA, DIA_DE_LA_SEMANA, TODO), cuaderno());

        assertThat(resultado.filas()).extracting(Fila::etiqueta)
                .containsExactly("Martes", "Miércoles", "Sin fecha");
    }

    @Test
    @DisplayName("Si hay una tabla de gastos sin importe y otra con importe usa sólo la que tiene importe")
    void calcularShouldIgnoreExpensesTableWithoutAmount() {
        DatosDeProyecto datos = datos(List.of(), List.of(), List.of(
                gastosSinImporte(), gastos(gasto(300, "2026-11-10", "Luz"))));

        ResultadoMetrica resultado = calcular(definicion(GASTOS, SUMA, NINGUNA, MES), datos);

        assertThat(resultado.valor()).isEqualByComparingTo("300");
    }

    @Test
    @DisplayName("Suma lo que vale el stock y deja afuera el producto sin costo")
    void calcularShouldSumStockValue() {
        ResultadoMetrica resultado = calcular(definicion(INVENTARIO, SUMA, NINGUNA, TODO), cuaderno());

        assertThat(resultado.valor()).isEqualByComparingTo("2000");
        assertThat(resultado.deDondeSale().filasSinDato()).isEqualTo(1);
    }

    @Test
    @DisplayName("Suma las unidades en stock repartidas por producto")
    void calcularShouldSplitStockUnitsByProduct() {
        ResultadoMetrica resultado = calcular(definicion(INVENTARIO, UNIDADES, PRODUCTO, TODO), cuaderno());

        assertThat(resultado.valor()).isEqualByComparingTo("19");
        assertThat(resultado.filas()).extracting(Fila::etiqueta).containsExactly("Pan", "Leche", "Sal");
    }

    @Test
    @DisplayName("Si el cuaderno no tiene tabla de ventas lo dice y no inventa un cero")
    void calcularShouldExplainWhenThereIsNoSalesTable() {
        DatosDeProyecto sinTablas = datos(List.of(), List.of(), List.of());

        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, NINGUNA, MES), sinTablas);

        assertThat(resultado.calculable()).isFalse();
        assertThat(resultado.valor()).isNull();
        assertThat(resultado.motivo()).isEqualTo("No encontramos la tabla de ventas en este cuaderno.");
    }

    @Test
    @DisplayName("Si la tabla de gastos no tiene importe es como si no hubiera tabla")
    void calcularShouldExplainWhenExpensesHaveNoAmountColumn() {
        DatosDeProyecto datos = datos(List.of(), List.of(), List.of(gastosSinImporte()));

        ResultadoMetrica resultado = calcular(definicion(GASTOS, SUMA, NINGUNA, MES), datos);

        assertThat(resultado.motivo()).isEqualTo("No encontramos la tabla de gastos en este cuaderno.");
    }

    @Test
    @DisplayName("Si no hay inventario lo dice")
    void calcularShouldExplainWhenThereIsNoInventory() {
        ResultadoMetrica resultado = calcular(
                definicion(INVENTARIO, SUMA, NINGUNA, TODO), datos(List.of(), List.of(), List.of()));

        assertThat(resultado.motivo()).isEqualTo("No encontramos la tabla de inventario en este cuaderno.");
    }

    @Test
    @DisplayName("Si no hay filas en el período lo dice")
    void calcularShouldExplainWhenThereAreNoRowsInThePeriod() {
        DatosDeProyecto datos = datos(List.of(inventario()), List.of(ventas(
                venta(PAN_ID, 1, 100, "2026-09-01", null, false))), List.of());

        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, NINGUNA, SEMANA), datos);

        assertThat(resultado.motivo()).isEqualTo("No hay ventas en este período.");
        assertThat(resultado.deDondeSale().filasUsadas()).isZero();
    }

    @Test
    @DisplayName("Si no hay ventas fiadas lo dice")
    void calcularShouldExplainWhenThereAreNoCreditSales() {
        DatosDeProyecto datos = datos(List.of(inventario()), List.of(ventas(
                venta(PAN_ID, 1, 100, "2026-11-10", null, false))), List.of());

        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, CLIENTE, MES, true), datos);

        assertThat(resultado.motivo()).isEqualTo("No hay ventas fiadas en este período.");
    }

    @Test
    @DisplayName("Si la métrica mira todo y no hay filas dice que todavía no cargó nada")
    void calcularShouldExplainWhenThereIsNothingLoadedYet() {
        DatosDeProyecto datos = datos(List.of(inventario()), List.of(ventas()), List.of());

        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, NINGUNA, TODO), datos);

        assertThat(resultado.motivo()).isEqualTo("Todavía no cargaste ventas.");
    }

    @Test
    @DisplayName("Si ninguna venta tiene precio pide cargarlo")
    void calcularShouldAskForPricesWhenNoSaleHasOne() {
        DatosDeProyecto datos = datos(List.of(inventario()), List.of(ventas(
                venta(PAN_ID, 1, null, "2026-11-10", null, false))), List.of());

        ResultadoMetrica resultado = calcular(definicion(VENTAS, SUMA, NINGUNA, MES), datos);

        assertThat(resultado.motivo()).isEqualTo("Cargá el precio de tus ventas para poder calcularlo.");
        assertThat(resultado.deDondeSale().filasSinDato()).isEqualTo(1);
    }

    @Test
    @DisplayName("Si ningún gasto tiene importe pide cargarlo")
    void calcularShouldAskForAmountsWhenNoExpenseHasOne() {
        DatosDeProyecto datos = datos(List.of(), List.of(), List.of(gastos(gasto(null, "2026-11-10", "Luz"))));

        ResultadoMetrica resultado = calcular(definicion(GASTOS, SUMA, NINGUNA, MES), datos);

        assertThat(resultado.motivo()).isEqualTo("Cargá el importe de tus gastos para poder calcularlo.");
    }

    @Test
    @DisplayName("Si ningún producto tiene costo pide cargarlo")
    void calcularShouldAskForCostsWhenNoProductHasOne() {
        BloqueInventario sinCosto = inventario();
        DatosDeProyecto datos = datos(List.of(new BloqueInventario(sinCosto.bloque(), null,
                sinCosto.camposPorClave(), sinCosto.registros().subList(2, 3))), List.of(), List.of());

        ResultadoMetrica resultado = calcular(definicion(INVENTARIO, SUMA, NINGUNA, TODO), datos);

        assertThat(resultado.motivo()).isEqualTo("Cargá el costo de tus productos para poder calcularlo.");
    }

    @Test
    @DisplayName("Si ningún producto tiene cantidad pide cargarla")
    void calcularShouldAskForQuantitiesWhenNoProductHasOne() {
        DatosDeProyecto datos = datos(List.of(inventarioSinCantidad()), List.of(), List.of());

        ResultadoMetrica resultado = calcular(definicion(INVENTARIO, UNIDADES, NINGUNA, TODO), datos);

        assertThat(resultado.motivo()).isEqualTo("Cargá la cantidad de tus productos para poder calcularlo.");
    }

    // --- act ---
    private ResultadoMetrica calcular(DefinicionMetrica definicion, DatosDeProyecto datos) {
        return motor.calcular(definicion, datos, HOY);
    }

    // --- helpers ---
    /**
     * Ventas: Pan $300 hoy (fiado, Ana), Leche $250 ayer (Beto), Pan $450 el 4/11 (sin cliente), Sal sin precio hoy,
     * una venta ilegible y Pan $150 de septiembre (fiado, Ana). Gastos con importes y fechas dudosas.
     */
    private static DatosDeProyecto cuaderno() {
        BloqueVentas ventas = ventas(
                venta(PAN_ID, 2, 150, "2026-11-11", "Ana", true),
                venta(LECHE_ID, 1, 250, "2026-11-10", "Beto", false),
                venta(PAN_ID, 3, 150, "2026-11-04", null, false),
                venta(SAL_ID, 1, null, "2026-11-11", "Ana", null),
                venta(null, 1, 100, "2026-11-11", "Ana", false),
                venta(PAN_ID, 1, 150, "2026-09-01", "Ana", true));
        BloqueGastos gastos = gastos(
                gasto(1000, "2026-11-10", "Alquiler"),
                gasto(200, "2026-11-11", " "),
                gasto(null, "2026-11-09", "Luz"),
                gasto("abc", "2026-11-08", "Luz"),
                gasto(50, "mal", "Luz"),
                gasto(25, 7, "Luz"));
        return datos(List.of(inventario()), List.of(ventas), List.of(gastos));
    }
}
