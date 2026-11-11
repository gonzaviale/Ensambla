package com.ensambla.backend.domain.model.pedido;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    @DisplayName("Cambiar de marca mantiene la cantidad, usa el precio de la nueva y deja volver a la anterior")
    void cambiarPorShouldKeepTheQuantityAndOfferTheOldOne() {
        ProductoEnStock natura = producto("Aceite Natura 900 ml", "2400", "8");
        ProductoEnStock lira = producto("Aceite Lira 900 ml", "2200", "5");
        LineaDeCompra linea = LineaDeCompra.de(ACEITE, BigDecimal.valueOf(2), List.of(natura, lira));

        LineaDeCompra cambiada = linea.cambiarPor(natura);

        assertThat(cambiada.producto()).isEqualTo(natura);
        assertThat(cambiada.cantidad()).isEqualByComparingTo("2");
        assertThat(cambiada.subtotal()).isEqualByComparingTo("4800");
        assertThat(cambiada.alternativas()).containsExactly(lira, ACEITE);
        assertThat(cambiada.limitadaPorStock()).isFalse();
    }

    @Test
    @DisplayName("Cambiar por una marca con menos stock deja el stock de la nueva y lo marca")
    void cambiarPorShouldCapTheQuantityAtTheNewStock() {
        ProductoEnStock escaso = producto("Aceite Lira 900 ml", "2200", "1");

        LineaDeCompra cambiada = LineaDeCompra.de(ACEITE, BigDecimal.valueOf(3), List.of(escaso)).cambiarPor(escaso);

        assertThat(cambiada.cantidad()).isEqualByComparingTo("1");
        assertThat(cambiada.limitadaPorStock()).isTrue();
    }

    @Test
    @DisplayName("El renglón sigue vigente si el producto tiene el mismo precio y stock para la cantidad")
    void sigueVigenteShouldCheckPriceAndStock() {
        LineaDeCompra linea = LineaDeCompra.de(ACEITE, BigDecimal.valueOf(3), List.of());
        ProductoEnStock igual = new ProductoEnStock(ACEITE.id(), ACEITE.nombre(), ACEITE.precio(), ACEITE.stock());
        ProductoEnStock masCaro = new ProductoEnStock(ACEITE.id(), ACEITE.nombre(), new BigDecimal("2100"), ACEITE.stock());
        ProductoEnStock sinStock = new ProductoEnStock(ACEITE.id(), ACEITE.nombre(), ACEITE.precio(), BigDecimal.TWO);

        assertThat(linea.sigueVigente(Map.of(ACEITE.id(), igual))).isTrue();
        assertThat(linea.sigueVigente(Map.of(ACEITE.id(), masCaro))).isFalse();
        assertThat(linea.sigueVigente(Map.of(ACEITE.id(), sinStock))).isFalse();
        assertThat(linea.sigueVigente(Map.of())).isFalse();
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
