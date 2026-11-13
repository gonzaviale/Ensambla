package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo.Gasto;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo.Producto;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.support.NegocioDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.NegocioDePrueba.ACEITE;
import static com.ensambla.backend.support.NegocioDePrueba.FIDEOS;
import static com.ensambla.backend.support.NegocioDePrueba.YERBA;
import static com.ensambla.backend.support.NegocioDePrueba.almacen;
import static org.assertj.core.api.Assertions.assertThat;

class MedidorMiniObjetivoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
    private static final UUID GALLETITAS = UUID.fromString("d0000000-0000-4000-8000-000000000004");
    private static final RegistroId YERBA_ID = RegistroId.de(YERBA);
    private static final RegistroId FIDEOS_ID = RegistroId.de(FIDEOS);
    private static final RegistroId ACEITE_ID = RegistroId.de(ACEITE);
    private static final RegistroId INEXISTENTE_ID = RegistroId.de("d0000000-0000-4000-8000-0000000000ff");

    @Test
    @DisplayName("Hay ventas cuando se vendió algo en los últimos 30 días")
    void hayVentasShouldBeTrueWhenThereWereSalesInTheLastThirtyDays() {
        assertThat(medidor(almacen()).hayVentas()).isTrue();
    }

    @Test
    @DisplayName("Sin ventas en los últimos 30 días no hay ventas ni resultado")
    void hayVentasShouldBeFalseWhenThereWereNoSalesInTheLastThirtyDays() {
        DatosDeProyecto viejo = NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .venta(YERBA, 10, 4200, "2026-08-01")
                .build();

        MedidorMiniObjetivo result = medidor(viejo);

        assertThat(result.hayVentas()).isFalse();
        assertThat(result.resultado()).isEqualByComparingTo("0");
        assertThat(result.margen()).isEmpty();
    }

    @Test
    @DisplayName("El resultado es lo vendido menos la mercadería y los gastos sin la categoría Mercadería, de 30 días")
    void resultadoShouldBeWhatIsLeftAfterMerchandiseAndExpenses() {
        MedidorMiniObjetivo result = medidor(almacen());

        assertThat(result.vendiste()).isEqualTo(309_000);
        assertThat(result.resultado()).isEqualByComparingTo("-27000");
    }

    @Test
    @DisplayName("El margen es lo que queda de cada $100 vendidos después de pagar la mercadería")
    void margenShouldBeWhatIsLeftOfEveryHundredAfterMerchandise() {
        Optional<BigDecimal> result = medidor(almacen()).margen();

        assertThat(result).hasValueSatisfying(margen -> assertThat(margen).isEqualByComparingTo("23.62"));
    }

    @Test
    @DisplayName("Sin costos cargados no hay margen con qué calcularlo")
    void margenShouldBeEmptyWhenThereAreNoCosts() {
        DatosDeProyecto sinCostos = NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, null)
                .venta(YERBA, 10, 4200, "2026-11-08")
                .build();

        assertThat(medidor(sinCostos).margen()).isEmpty();
    }

    @Test
    @DisplayName("Las ventas de la semana son lo vendido con precio en los últimos 7 días")
    void ventasSemanaShouldAddThePricedSalesOfTheLastSevenDays() {
        assertThat(medidor(almacen()).ventasSemana()).isEqualByComparingTo("73050");
    }

    @Test
    @DisplayName("Las ventas de la semana no cuentan las que no tienen precio")
    void ventasSemanaShouldIgnoreSalesWithoutPrice() {
        DatosDeProyecto sinPrecio = NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .venta(YERBA, 2, null, "2026-11-10")
                .build();

        assertThat(medidor(sinPrecio).ventasSemana()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Las unidades de un producto cuentan sólo la ventana pedida, hoy incluido")
    void unidadesShouldCountOnlyTheRequestedWindow() {
        MedidorMiniObjetivo result = medidor(almacen());

        assertThat(result.unidades(YERBA_ID, MedidorMiniObjetivo.DIAS_SEMANA)).isEqualByComparingTo("14");
        assertThat(result.unidades(YERBA_ID, MedidorMiniObjetivo.DIAS_MES)).isEqualByComparingTo("60");
        assertThat(result.unidades(FIDEOS_ID, MedidorMiniObjetivo.DIAS_MES)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("La ganancia de la semana es lo vendido al precio de cada venta menos el costo actual")
    void gananciaSemanalShouldUseTheSalePriceMinusTheCurrentCost() {
        Producto yerba = productoDe(medidor(almacen()), YERBA_ID);

        assertThat(medidor(almacen()).gananciaSemanal(yerba)).isEqualByComparingTo("12600");
    }

    @Test
    @DisplayName("La ganancia de una venta sin precio usa el precio actual del producto")
    void gananciaSemanalShouldUseTheCurrentPriceWhenTheSaleHasNone() {
        DatosDeProyecto sinPrecio = NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .venta(YERBA, 2, null, "2026-11-10")
                .build();
        MedidorMiniObjetivo medidor = medidor(sinPrecio);

        assertThat(medidor.gananciaSemanal(productoDe(medidor, YERBA_ID))).isEqualByComparingTo("1800");
    }

    @Test
    @DisplayName("Los gastos son los de cada categoría en 30 días, sin la mercadería y sin lo anterior")
    void gastosShouldBeThoseOfEachCategoryWithoutMerchandise() {
        List<Gasto> result = medidor(almacen()).gastos();

        assertThat(result).extracting(Gasto::categoria).containsExactly("Alquiler", "Servicios");
        assertThat(result).extracting(Gasto::total).satisfiesExactly(
                total -> assertThat(total).isEqualByComparingTo("60000"),
                total -> assertThat(total).isEqualByComparingTo("40000"));
    }

    @Test
    @DisplayName("Suma la misma categoría de distintos cuadernos aunque esté escrita distinto, y salta lo que no gastó")
    void gastosShouldAddTheSameCategoryAcrossNotebooks() {
        DatosDeProyecto uno = conVentaYGasto("Almacén", "Servicios", 30000);
        DatosDeProyecto otro = conVentaYGasto("Kiosco", " servicios ", 12000);
        DatosDeProyecto sinTabla = NegocioDePrueba.cuaderno("Feria").sinTablaDeGastos().build();
        DatosDeProyecto sinGasto = conVentaYGasto("Puesto", "Limpieza", 0);

        List<Gasto> result = MedidorMiniObjetivo.de(List.of(uno, otro, sinTabla, sinGasto), metricas(), HOY).gastos();

        assertThat(result).singleElement().satisfies(gasto -> {
            assertThat(gasto.categoria()).isEqualTo("Servicios");
            assertThat(gasto.total()).isEqualByComparingTo("42000");
        });
    }

    @Test
    @DisplayName("Sólo sirven los productos con nombre, precio y costo; sin stock cargado cuenta cero")
    void productosShouldKeepOnlyThoseWithNamePriceAndCost() {
        DatosDeProyecto incompletos = NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", null, 4200, 3300)
                .producto(FIDEOS, "  ", 10, 1100, 800)
                .producto(ACEITE, null, 10, 2850, 1900)
                .producto(GALLETITAS, "Galletitas", 10, null, 600)
                .producto(UUID.fromString("d0000000-0000-4000-8000-000000000005"), "Alfajor", 10, 900, null)
                .producto(UUID.fromString("d0000000-0000-4000-8000-000000000006"), "Regalo", 10, 0, 0)
                .build();

        List<Producto> result = medidor(incompletos).productos();

        assertThat(result).singleElement().satisfies(producto -> {
            assertThat(producto.id()).isEqualTo(YERBA_ID);
            assertThat(producto.nombre()).isEqualTo("Yerba 1 kg");
            assertThat(producto.stock()).isEqualByComparingTo("0");
            assertThat(producto.gananciaPorUnidad()).isEqualByComparingTo("900");
        });
    }

    @Test
    @DisplayName("Subir un precio se mide con la ganancia semanal del producto")
    void valorDeShouldMeasureTheWeeklyProfitOfAProduct() {
        Optional<BigDecimal> result = medidor(almacen()).valorDe(TipoMiniObjetivo.SUBIR_PRECIO, YERBA_ID, "Yerba 1 kg");

        assertThat(result).hasValueSatisfying(valor -> assertThat(valor).isEqualByComparingTo("12600"));
    }

    @Test
    @DisplayName("Mover un parado y poner a la vista se miden con las unidades de la semana del producto")
    void valorDeShouldMeasureTheWeeklyUnitsOfAProduct() {
        MedidorMiniObjetivo medidor = medidor(almacen());

        assertThat(medidor.valorDe(TipoMiniObjetivo.MOVER_PARADO, FIDEOS_ID, "Fideos 500 g"))
                .hasValueSatisfying(valor -> assertThat(valor).isEqualByComparingTo("0"));
        assertThat(medidor.valorDe(TipoMiniObjetivo.PONER_A_LA_VISTA, ACEITE_ID, "Aceite 900 ml"))
                .hasValueSatisfying(valor -> assertThat(valor).isEqualByComparingTo("5"));
    }

    @Test
    @DisplayName("Un producto que ya no está en el cuaderno, o sin producto, no se puede medir")
    void valorDeShouldBeEmptyWhenTheProductIsGone() {
        MedidorMiniObjetivo medidor = medidor(almacen());

        assertThat(medidor.valorDe(TipoMiniObjetivo.SUBIR_PRECIO, INEXISTENTE_ID, "Algo")).isEmpty();
        assertThat(medidor.valorDe(TipoMiniObjetivo.MOVER_PARADO, null, "Algo")).isEmpty();
    }

    @Test
    @DisplayName("Recortar un gasto se mide con lo gastado en la categoría, sin importar tildes ni mayúsculas")
    void valorDeShouldMeasureTheSpendingOfACategory() {
        assertThat(medidor(almacen()).valorDe(TipoMiniObjetivo.RECORTAR_GASTO, null, "SERVICIOS"))
                .hasValueSatisfying(valor -> assertThat(valor).isEqualByComparingTo("40000"));
    }

    @Test
    @DisplayName("Una categoría sin gastos en 30 días se mide como cero")
    void valorDeShouldBeZeroForACategoryWithoutExpenses() {
        assertThat(medidor(almacen()).valorDe(TipoMiniObjetivo.RECORTAR_GASTO, null, "Impuestos"))
                .hasValueSatisfying(valor -> assertThat(valor).isEqualByComparingTo("0"));
    }

    @Test
    @DisplayName("Mejorar el margen y vender más por semana se miden con los números del negocio")
    void valorDeShouldMeasureTheBusinessMetrics() {
        MedidorMiniObjetivo medidor = medidor(almacen());

        assertThat(medidor.valorDe(TipoMiniObjetivo.MEJORAR_MARGEN, null, null))
                .hasValueSatisfying(valor -> assertThat(valor).isEqualByComparingTo("23.62"));
        assertThat(medidor.valorDe(TipoMiniObjetivo.VENTAS_SEMANALES, null, null))
                .hasValueSatisfying(valor -> assertThat(valor).isEqualByComparingTo("73050"));
    }

    @Test
    @DisplayName("El resultado por semana trae el de los 30 días que terminan cada semana, por su lunes, hasta hoy")
    void resultadoPorSemanaShouldHaveTheResultOfEachOfTheLastWeeks() {
        Map<LocalDate, BigDecimal> result = medidor(almacen()).resultadoPorSemana();

        assertThat(result).hasSize(ComoVieneElNegocio.SEMANAS_DE_HISTORIAL);
        assertThat(result.keySet()).first().isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(result.keySet()).last().isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(result.get(LocalDate.of(2026, 11, 9))).isEqualByComparingTo("-27000");
    }

    @Test
    @DisplayName("Sin ventas no hay resultado por semana")
    void resultadoPorSemanaShouldBeEmptyWithoutSales() {
        DatosDeProyecto sinVentas = NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .build();

        assertThat(medidor(sinVentas).resultadoPorSemana()).isEmpty();
    }

    // --- helpers: negocio ---
    private static DatosDeProyecto conVentaYGasto(String cuaderno, String categoria, int total) {
        return NegocioDePrueba.cuaderno(cuaderno)
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .venta(YERBA, 1, 4200, "2026-11-08")
                .gasto("2026-11-01", total, categoria)
                .build();
    }

    private static MedidorMiniObjetivo medidor(DatosDeProyecto datos) {
        return MedidorMiniObjetivo.de(List.of(datos), metricas(), HOY);
    }

    private static MetricasVentas metricas() {
        return new MetricasVentas(30, new BigDecimal("0.5"));
    }

    private static Producto productoDe(MedidorMiniObjetivo medidor, RegistroId id) {
        return medidor.productos().stream().filter(producto -> producto.id().equals(id)).findFirst().orElseThrow();
    }
}
