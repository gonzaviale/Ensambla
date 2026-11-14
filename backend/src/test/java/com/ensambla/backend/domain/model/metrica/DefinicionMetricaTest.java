package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefinicionMetricaTest {

    @Test
    @DisplayName("Acepta una receta válida")
    void validarShouldAcceptValidDefinition() {
        DefinicionMetrica definicion = definicion(
                "Lo que te deben", FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.CLIENTE, true);

        assertThatCode(definicion::validar).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Sin reparto ni período elige no repartir y mirar el mes")
    void constructorShouldDefaultGroupingAndPeriod() {
        DefinicionMetrica definicion = new DefinicionMetrica(
                "  Ventas ", FuenteMetrica.VENTAS, CalculoMetrica.SUMA, null, null, false);

        assertThat(definicion.nombre()).isEqualTo("Ventas");
        assertThat(definicion.agrupar()).isEqualTo(AgrupacionMetrica.NINGUNA);
        assertThat(definicion.periodo()).isEqualTo(PeriodoMetrica.MES);
    }

    @Test
    @DisplayName("Sin nombre la receta no es válida")
    void validarShouldRejectBlankName() {
        DefinicionMetrica definicion = definicion(null, FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                AgrupacionMetrica.NINGUNA, false);

        assertThatThrownBy(definicion::validar).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    @DisplayName("Un nombre muy largo no es válido")
    void validarShouldRejectLongName() {
        DefinicionMetrica definicion = definicion("a".repeat(DefinicionMetrica.LARGO_MAXIMO_DEL_NOMBRE + 1),
                FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA, false);

        assertThatThrownBy(definicion::validar).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    @DisplayName("Sin tabla la receta no es válida")
    void validarShouldRejectMissingSource() {
        DefinicionMetrica definicion = definicion("Algo", null, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA, false);

        assertThatThrownBy(definicion::validar).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("de qué tabla");
    }

    @Test
    @DisplayName("Sin cálculo la receta no es válida")
    void validarShouldRejectMissingCalculation() {
        DefinicionMetrica definicion = definicion("Algo", FuenteMetrica.VENTAS, null, AgrupacionMetrica.NINGUNA, false);

        assertThatThrownBy(definicion::validar).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("de qué tabla");
    }

    @Test
    @DisplayName("Los gastos no tienen unidades")
    void validarShouldRejectUnitsOfExpenses() {
        DefinicionMetrica definicion = definicion(
                "Algo", FuenteMetrica.GASTOS, CalculoMetrica.UNIDADES, AgrupacionMetrica.NINGUNA, false);

        assertThatThrownBy(definicion::validar).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("unidades");
    }

    @Test
    @DisplayName("No se puede repartir las ventas por categoría")
    void validarShouldRejectGroupingThatDoesNotApply() {
        DefinicionMetrica definicion = definicion(
                "Algo", FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.CATEGORIA, false);

        assertThatThrownBy(definicion::validar).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("repartir ventas");
    }

    @Test
    @DisplayName("Sólo las ventas pueden ser fiadas")
    void validarShouldRejectCreditOutsideSales() {
        DefinicionMetrica definicion = definicion(
                "Algo", FuenteMetrica.GASTOS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA, true);

        assertThatThrownBy(definicion::validar).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("fiadas");
    }

    @Test
    @DisplayName("El inventario no tiene períodos")
    void validarShouldRejectPeriodsOfInventory() {
        DefinicionMetrica definicion = new DefinicionMetrica(
                "Algo", FuenteMetrica.INVENTARIO, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA,
                PeriodoMetrica.SEMANA, false);

        assertThatThrownBy(definicion::validar).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("períodos");
    }

    @Test
    @DisplayName("Acepta el inventario de todo lo cargado repartido por producto")
    void validarShouldAcceptInventoryByProduct() {
        DefinicionMetrica definicion = new DefinicionMetrica(
                "Stock", FuenteMetrica.INVENTARIO, CalculoMetrica.UNIDADES, AgrupacionMetrica.PRODUCTO,
                PeriodoMetrica.TODO, false);

        assertThatCode(definicion::validar).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Acepta los gastos repartidos por categoría")
    void validarShouldAcceptExpensesByCategory() {
        DefinicionMetrica definicion = definicion(
                "Gastos", FuenteMetrica.GASTOS, CalculoMetrica.SUMA, AgrupacionMetrica.CATEGORIA, false);

        assertThatCode(definicion::validar).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("El formato sale del cálculo: pesos o cantidad")
    void formatoShouldComeFromTheCalculation() {
        assertThat(definicion("A", FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA, false)
                .formato()).isEqualTo(FormatoMetrica.MONEDA);
        assertThat(definicion("A", FuenteMetrica.VENTAS, CalculoMetrica.CONTEO, AgrupacionMetrica.NINGUNA, false)
                .formato()).isEqualTo(FormatoMetrica.NUMERO);
    }

    @Test
    @DisplayName("Dos recetas son la misma aunque se llamen distinto")
    void esIgualAShouldIgnoreTheName() {
        DefinicionMetrica una = definicion("Una", FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                AgrupacionMetrica.NINGUNA, false);
        DefinicionMetrica otra = definicion("Otra", FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                AgrupacionMetrica.NINGUNA, false);

        assertThat(una.esIgualA(otra)).isTrue();
    }

    @Test
    @DisplayName("Dos recetas se distinguen por cada cosa que piden")
    void esIgualAShouldDetectEachDifference() {
        DefinicionMetrica base = definicion("A", FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                AgrupacionMetrica.NINGUNA, false);

        assertThat(base.esIgualA(definicion("A", FuenteMetrica.GASTOS, CalculoMetrica.SUMA,
                AgrupacionMetrica.NINGUNA, false))).isFalse();
        assertThat(base.esIgualA(definicion("A", FuenteMetrica.VENTAS, CalculoMetrica.CONTEO,
                AgrupacionMetrica.NINGUNA, false))).isFalse();
        assertThat(base.esIgualA(definicion("A", FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                AgrupacionMetrica.CLIENTE, false))).isFalse();
        assertThat(base.esIgualA(new DefinicionMetrica("A", FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                AgrupacionMetrica.NINGUNA, PeriodoMetrica.SEMANA, false))).isFalse();
        assertThat(base.esIgualA(definicion("A", FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                AgrupacionMetrica.NINGUNA, true))).isFalse();
    }

    // --- helpers ---
    private static DefinicionMetrica definicion(
            String nombre, FuenteMetrica fuente, CalculoMetrica calculo, AgrupacionMetrica agrupar,
            boolean soloFiado) {
        return new DefinicionMetrica(nombre, fuente, calculo, agrupar, PeriodoMetrica.MES, soloFiado);
    }
}
