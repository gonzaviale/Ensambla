package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.reporte.AvisoEconomico.Nivel;
import com.ensambla.backend.domain.model.reporte.AvisoEconomico.Tendencia;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ProductoVendido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvisoEconomicoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);

    @Test
    @DisplayName("No avisa nada si la semana anterior no tuvo ventas")
    void compararShouldReturnNullWhenThePreviousWeekHasNoSales() {
        AvisoEconomico aviso = whenComparar(
                resumen("1000", "300", List.of(producto("Yerba", "1000", "300"))),
                resumen("0", null, List.of()));

        assertThat(aviso).isNull();
    }

    @Test
    @DisplayName("Avisa con prioridad alta cuando vendió más pero ganó menos, con los productos que más bajaron")
    void compararShouldWarnWhenSalesGoUpButProfitGoesDown() {
        AvisoEconomico aviso = whenComparar(
                resumen("12000", "2700", List.of(
                        producto("Yerba", "6000", "1000"),
                        producto("Aceite", "4000", "1200"),
                        producto("Leche", "2000", "500"))),
                resumen("10000", "3000", List.of(
                        producto("Yerba", "5000", "1500"),
                        producto("Aceite", "3000", "1000"),
                        producto("Leche", "2000", "500"))));

        assertThat(aviso.prioridad()).isEqualTo(Nivel.ALTA);
        assertThat(aviso.tendencia()).isEqualTo(Tendencia.EMPEORA);
        assertThat(aviso.categoria()).isEqualTo("Rentabilidad");
        assertThat(aviso.titulo()).isEqualTo("Las ventas subieron 20 %, pero la ganancia bajó 10 %");
        assertThat(aviso.explicacion()).startsWith("Vendiste más, pero te quedó menos ganancia");
        assertThat(aviso.variacionVentas()).isEqualByComparingTo("20");
        assertThat(aviso.variacionResultadoBruto()).isEqualByComparingTo("-10");
        assertThat(aviso.productos()).containsExactly("Yerba");
        assertThat(aviso.confianza()).isEqualTo(Nivel.ALTA);
    }

    @Test
    @DisplayName("Avisa con prioridad media cuando vendió menos y ganó menos")
    void compararShouldWarnWhenSalesAndProfitGoDown() {
        AvisoEconomico aviso = whenComparar(
                resumen("8000", "2000", List.of(producto("Yerba", "8000", "2000"))),
                resumen("10000", "3000", List.of(producto("Yerba", "10000", "3000"))));

        assertThat(aviso.prioridad()).isEqualTo(Nivel.MEDIA);
        assertThat(aviso.tendencia()).isEqualTo(Tendencia.EMPEORA);
        assertThat(aviso.titulo()).isEqualTo("Las ventas bajaron 20 % y la ganancia bajó 33 %");
        assertThat(aviso.explicacion()).startsWith("Esta semana vendiste menos y te quedó menos ganancia");
    }

    @Test
    @DisplayName("Avisa que está mejorando cuando vendió más y ganó más, con los productos que más subieron")
    void compararShouldSayItIsImprovingWhenSalesAndProfitGoUp() {
        AvisoEconomico aviso = whenComparar(
                resumen("11000", "3300", List.of(
                        producto("Yerba", "6000", "1800"),
                        producto("Fideos", "5000", "1500"))),
                resumen("10000", "3000", List.of(
                        producto("Yerba", "5000", "1500"),
                        producto("Fideos", "5000", "1500"))));

        assertThat(aviso.prioridad()).isEqualTo(Nivel.BAJA);
        assertThat(aviso.tendencia()).isEqualTo(Tendencia.MEJORA);
        assertThat(aviso.titulo()).isEqualTo("Las ventas subieron 10 % y la ganancia subió 10 %");
        assertThat(aviso.explicacion()).startsWith("Tu negocio está mejorando: vendiste más y te quedó más ganancia");
        assertThat(aviso.productos()).containsExactly("Yerba");
    }

    @Test
    @DisplayName("Avisa que está mejorando cuando vendió menos pero ganó más")
    void compararShouldSayItIsImprovingWhenSalesGoDownButProfitGoesUp() {
        AvisoEconomico aviso = whenComparar(
                resumen("9000", "3600", List.of(producto("Yerba", "9000", "3600"))),
                resumen("10000", "3000", List.of(producto("Yerba", "10000", "3000"))));

        assertThat(aviso.prioridad()).isEqualTo(Nivel.BAJA);
        assertThat(aviso.tendencia()).isEqualTo(Tendencia.MEJORA);
        assertThat(aviso.titulo()).isEqualTo("Las ventas bajaron 10 %, pero la ganancia subió 20 %");
        assertThat(aviso.explicacion()).startsWith("Vendiste menos, pero te quedó más ganancia");
    }

    @Test
    @DisplayName("Sin costos compara sólo las ventas, pide cargar los costos y ordena los productos por lo vendido")
    void compararShouldUseOnlySalesWhenThereAreNoCosts() {
        AvisoEconomico aviso = whenComparar(
                resumen("12000", null, List.of(
                        producto("Yerba", "7000", null),
                        producto("Fideos", "5000", null))),
                resumen("10000", null, List.of(
                        producto("Yerba", "5000", null),
                        producto("Fideos", "5000", null))));

        assertThat(aviso.prioridad()).isEqualTo(Nivel.BAJA);
        assertThat(aviso.tendencia()).isEqualTo(Tendencia.MEJORA);
        assertThat(aviso.categoria()).isEqualTo("Ventas");
        assertThat(aviso.titulo()).isEqualTo("Las ventas subieron 20 % frente a la semana pasada");
        assertThat(aviso.explicacion())
                .isEqualTo("Tu negocio está mejorando: vendiste más que la semana pasada."
                        + " Cargá el costo de tus productos para saber también cuánto ganaste.");
        assertThat(aviso.variacionResultadoBruto()).isNull();
        assertThat(aviso.productos()).containsExactly("Yerba");
        assertThat(aviso.confianza()).isEqualTo(Nivel.BAJA);
    }

    @Test
    @DisplayName("Sin costos y con menos ventas avisa con prioridad media")
    void compararShouldWarnWhenSalesGoDownWithoutCosts() {
        AvisoEconomico aviso = whenComparar(
                resumen("0", null, List.of()),
                resumen("10000", null, List.of(producto("Yerba", "10000", null))));

        assertThat(aviso.prioridad()).isEqualTo(Nivel.MEDIA);
        assertThat(aviso.tendencia()).isEqualTo(Tendencia.EMPEORA);
        assertThat(aviso.titulo()).isEqualTo("Las ventas bajaron 100 % frente a la semana pasada");
        assertThat(aviso.explicacion()).startsWith("Esta semana vendiste menos que la anterior.");
        assertThat(aviso.productos()).containsExactly("Yerba");
        assertThat(aviso.confianza()).isEqualTo(Nivel.BAJA);
    }

    @Test
    @DisplayName("No calcula la ganancia si la semana anterior no tiene costos")
    void compararShouldSkipProfitWhenThePreviousWeekHasNoCosts() {
        AvisoEconomico aviso = whenComparar(
                resumen("12000", "3000", List.of(producto("Yerba", "12000", "3000"))),
                resumen("10000", null, List.of(producto("Yerba", "10000", null))));

        assertThat(aviso.variacionResultadoBruto()).isNull();
    }

    @Test
    @DisplayName("No calcula la ganancia si la semana anterior no dejó ganancia")
    void compararShouldSkipProfitWhenThePreviousWeekHadNoProfit() {
        AvisoEconomico aviso = whenComparar(
                resumen("12000", "3000", List.of(producto("Yerba", "12000", "3000"))),
                resumen("10000", "0", List.of(producto("Yerba", "10000", "0"))));

        assertThat(aviso.variacionResultadoBruto()).isNull();
    }

    @Test
    @DisplayName("La confianza es media si sólo una parte de lo vendido tiene costo")
    void compararShouldReturnMediumConfidenceWhenSomeProductsHaveNoCost() {
        AvisoEconomico aviso = whenComparar(
                resumen("12000", "3000", List.of(
                        producto("Yerba", "4000", "1000"),
                        producto("Aceite", "4000", "2000"),
                        producto("Fideos", "4000", null))),
                resumen("10000", "2500", List.of(producto("Yerba", "10000", "2500"))));

        assertThat(aviso.confianza()).isEqualTo(Nivel.MEDIA);
    }

    @Test
    @DisplayName("La confianza es baja si casi nada de lo vendido tiene costo")
    void compararShouldReturnLowConfidenceWhenFewProductsHaveCost() {
        AvisoEconomico aviso = whenComparar(
                resumen("12000", "3000", List.of(
                        producto("Yerba", "4000", "3000"),
                        producto("Aceite", "4000", null),
                        producto("Fideos", "4000", null))),
                resumen("10000", "2500", List.of(producto("Yerba", "10000", "2500"))));

        assertThat(aviso.confianza()).isEqualTo(Nivel.BAJA);
    }

    @Test
    @DisplayName("Muestra hasta tres productos, primero los que más cambiaron y a igual cambio por nombre")
    void compararShouldLimitProductsToThreeOrderedByChange() {
        AvisoEconomico aviso = whenComparar(
                resumen("20000", "6000", List.of(
                        producto("Yerba", "5000", "2000"),
                        producto("Aceite", "5000", "1500"),
                        producto("Leche", "5000", "1500"),
                        producto("Fideos", "5000", "1000"))),
                resumen("10000", "2000", List.of()));

        assertThat(aviso.productos()).containsExactly("Yerba", "Aceite", "Leche");
    }

    // --- act ---
    private static AvisoEconomico whenComparar(ResumenNegocio actual, ResumenNegocio anterior) {
        return AvisoEconomico.comparar(actual, anterior);
    }

    // --- helpers ---
    private static ResumenNegocio resumen(String ingresos, String ganancia, List<ProductoVendido> vendidos) {
        ResumenNegocio.Ventas ventas = new ResumenNegocio.Ventas(new BigDecimal(ingresos), BigDecimal.ZERO, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, null, ganancia == null ? null : new BigDecimal(ganancia));
        return new ResumenNegocio("Almacén de Ana", "Almacén", new PeriodoResumen(HOY.minusDays(6), HOY), HOY,
                ventas, vendidos, List.of(), null, null, null, null, null);
    }

    private static ProductoVendido producto(String nombre, String ingresos, String ganancia) {
        return new ProductoVendido(nombre, BigDecimal.ONE, new BigDecimal(ingresos),
                ganancia == null ? null : new BigDecimal(ganancia));
    }
}
