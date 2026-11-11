package com.ensambla.backend.domain.model.pedido;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static com.ensambla.backend.support.PedidoTestData.producto;
import static org.assertj.core.api.Assertions.assertThat;

class BuscadorDeProductosTest {

    private static final ProductoEnStock YERBA_PLAYADITO = producto("Yerba Playadito 1 kg", "3500", "10");
    private static final ProductoEnStock YERBA_TARAGUI = producto("Yerba Taragüí 1 kg", "3800", "6");
    private static final ProductoEnStock ACEITE_COCINERO = producto("Aceite Cocinero 900 ml", "2000", "3");
    private static final ProductoEnStock ACEITE_NATURA = producto("Aceite Natura 900 ml", "2400", "8");
    private static final ProductoEnStock FIDEOS = producto("Fideos Matarazzo 500 g", "1200", "20");
    private static final List<ProductoEnStock> CATALOGO = List.of(ACEITE_COCINERO, ACEITE_NATURA, FIDEOS,
            YERBA_PLAYADITO, YERBA_TARAGUI);

    @Test
    @DisplayName("Un pedido claro agrega ese producto con la cantidad pedida y sin alternativas")
    void armarShouldAddTheClearlyNamedProduct() {
        ArmadoDeLista armado = BuscadorDeProductos.armar(
                List.of(new LineaPedida("yerba playadito", BigDecimal.valueOf(2))), CATALOGO);

        assertThat(armado.lineas()).singleElement().satisfies(linea -> {
            assertThat(linea.producto()).isEqualTo(YERBA_PLAYADITO);
            assertThat(linea.cantidad()).isEqualByComparingTo("2");
            assertThat(linea.alternativas()).isEmpty();
        });
        assertThat(armado.faltantes()).isEmpty();
        assertThat(armado.total()).isEqualByComparingTo("7000");
    }

    @Test
    @DisplayName("Un pedido vago elige la primera opción razonable y deja las otras marcas como alternativas")
    void armarShouldPickTheFirstOptionForAVagueRequest() {
        ArmadoDeLista armado = BuscadorDeProductos.armar(List.of(new LineaPedida("aceite", null)), CATALOGO);

        assertThat(armado.lineas()).singleElement().satisfies(linea -> {
            assertThat(linea.producto()).isEqualTo(ACEITE_COCINERO);
            assertThat(linea.cantidad()).isEqualByComparingTo("1");
            assertThat(linea.alternativas()).containsExactly(ACEITE_NATURA);
        });
    }

    @Test
    @DisplayName("Ofrece como mucho cuatro alternativas")
    void armarShouldCapTheAlternatives() {
        List<ProductoEnStock> galletitas = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> producto("Galletitas Marca" + i, "500", "10")).toList();

        ArmadoDeLista armado = BuscadorDeProductos.armar(List.of(new LineaPedida("galletitas", null)), galletitas);

        assertThat(armado.lineas().getFirst().alternativas()).hasSize(BuscadorDeProductos.MAX_ALTERNATIVAS);
    }

    @Test
    @DisplayName("Lo que el comercio no tiene se avisa y se recomienda el producto parecido que sí tiene")
    void armarShouldRecommendASimilarProductWhenMissing() {
        ArmadoDeLista armado = BuscadorDeProductos.armar(List.of(new LineaPedida("yerba mate cbse", null)), CATALOGO);

        assertThat(armado.lineas()).isEmpty();
        assertThat(armado.faltantes()).containsExactly(new ProductoFaltante("yerba mate cbse", YERBA_PLAYADITO));
    }

    @Test
    @DisplayName("Lo que no se parece a nada se avisa sin recomendar nada")
    void armarShouldReportMissingWithoutRecommendation() {
        ArmadoDeLista armado = BuscadorDeProductos.armar(List.of(new LineaPedida("pañales", null)), CATALOGO);

        assertThat(armado.faltantes()).containsExactly(new ProductoFaltante("pañales", null));
    }

    @Test
    @DisplayName("Mezcla lo que hay y lo que falta en el orden en que pidió")
    void armarShouldSeparateWhatThereIsFromWhatIsMissing() {
        ArmadoDeLista armado = BuscadorDeProductos.armar(List.of(
                new LineaPedida("fideos", BigDecimal.valueOf(2)), new LineaPedida("pañales", null),
                new LineaPedida("yerba taragui", null)), CATALOGO);

        assertThat(armado.lineas()).extracting(LineaDeCompra::producto).containsExactly(FIDEOS, YERBA_TARAGUI);
        assertThat(armado.faltantes()).extracting(ProductoFaltante::pedido).containsExactly("pañales");
    }

    @Test
    @DisplayName("Pedir dos veces lo mismo es un solo renglón con la cantidad sumada y limitada por el stock")
    void armarShouldMergeRepeatedProducts() {
        ArmadoDeLista armado = BuscadorDeProductos.armar(List.of(
                new LineaPedida("aceite cocinero", BigDecimal.valueOf(2)),
                new LineaPedida("aceite cocinero", BigDecimal.valueOf(4))), CATALOGO);

        assertThat(armado.lineas()).singleElement().satisfies(linea -> {
            assertThat(linea.cantidad()).isEqualByComparingTo("3");
            assertThat(linea.limitadaPorStock()).isTrue();
        });
    }

    @Test
    @DisplayName("Sin pedido o sin catálogo no hay lista")
    void armarShouldHandleEmptyInputs() {
        assertThat(BuscadorDeProductos.armar(List.of(), CATALOGO).lineas()).isEmpty();
        assertThat(BuscadorDeProductos.armar(List.of(new LineaPedida("yerba", null)), List.of()).faltantes())
                .hasSize(1);
    }
}
