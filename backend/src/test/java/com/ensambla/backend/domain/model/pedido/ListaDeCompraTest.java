package com.ensambla.backend.domain.model.pedido;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.PedidoTestData.producto;
import static org.assertj.core.api.Assertions.assertThat;

class ListaDeCompraTest {

    private static final ProductoEnStock YERBA = producto("Yerba Playadito 1 kg", "3500", "10");
    private static final ProductoEnStock ACEITE = producto("Aceite Cocinero 900 ml", "2000", "3");

    @Test
    @DisplayName("Pide la cantidad que dijo el cliente y calcula el subtotal con el precio del comercio")
    void deShouldUseTheRequestedQuantity() {
        LineaDeCompra linea = LineaDeCompra.de(YERBA, BigDecimal.valueOf(2), List.of());

        assertThat(linea.cantidad()).isEqualByComparingTo("2");
        assertThat(linea.limitadaPorStock()).isFalse();
        assertThat(linea.subtotal()).isEqualByComparingTo("7000");
    }

    @Test
    @DisplayName("Sin cantidad, o con una que no sirve, pide una unidad")
    void deShouldDefaultToOneUnit() {
        assertThat(LineaDeCompra.de(YERBA, null, List.of()).cantidad()).isEqualByComparingTo("1");
        assertThat(LineaDeCompra.de(YERBA, BigDecimal.ZERO, List.of()).cantidad()).isEqualByComparingTo("1");
        assertThat(LineaDeCompra.de(YERBA, BigDecimal.valueOf(-3), List.of()).cantidad()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("Pedir más de lo que hay deja lo que hay y lo marca")
    void deShouldCapTheQuantityAtTheStock() {
        LineaDeCompra linea = LineaDeCompra.de(ACEITE, BigDecimal.TEN, List.of());

        assertThat(linea.cantidad()).isEqualByComparingTo("3");
        assertThat(linea.limitadaPorStock()).isTrue();
        assertThat(linea.subtotal()).isEqualByComparingTo("6000");
    }

    @Test
    @DisplayName("Pedir exactamente el stock no se marca como limitado")
    void deShouldNotFlagTheExactStock() {
        assertThat(LineaDeCompra.de(ACEITE, BigDecimal.valueOf(3), List.of()).limitadaPorStock()).isFalse();
    }

    @Test
    @DisplayName("El mismo producto pedido dos veces suma las cantidades sin pasar el stock")
    void sumarShouldAddQuantitiesUpToTheStock() {
        LineaDeCompra dos = LineaDeCompra.de(ACEITE, BigDecimal.valueOf(2), List.of(YERBA));

        assertThat(dos.sumar(LineaDeCompra.de(ACEITE, BigDecimal.ONE, List.of())).cantidad()).isEqualByComparingTo("3");
        LineaDeCompra excedida = dos.sumar(LineaDeCompra.de(ACEITE, BigDecimal.valueOf(5), List.of()));
        assertThat(excedida.cantidad()).isEqualByComparingTo("3");
        assertThat(excedida.limitadaPorStock()).isTrue();
        assertThat(excedida.alternativas()).containsExactly(YERBA);
    }

    @Test
    @DisplayName("El total de la lista suma los subtotales y una lista vacía suma cero")
    void totalShouldAddTheSubtotals() {
        List<LineaDeCompra> lineas = List.of(LineaDeCompra.de(YERBA, BigDecimal.valueOf(2), List.of()),
                LineaDeCompra.de(ACEITE, BigDecimal.ONE, List.of()));

        assertThat(ArmadoDeLista.total(lineas)).isEqualByComparingTo("9000");
        assertThat(new ArmadoDeLista(lineas, List.of()).total()).isEqualByComparingTo("9000");
        assertThat(ArmadoDeLista.total(List.of())).isEqualByComparingTo("0");
    }
}
