package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.pedido.ConsultaPedido;
import com.ensambla.backend.domain.model.pedido.LineaPedida;
import com.ensambla.backend.domain.model.pedido.PedidoInterpretado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterpretePedidoSimuladoTest {

    private final InterpretePedidoSimulado interprete = new InterpretePedidoSimulado();

    @Test
    @DisplayName("Separa las cosas por comas y por «y», toma la cantidad del principio y deja el producto sin la unidad")
    void interpretarShouldSplitTheOrderIntoLines() {
        PedidoInterpretado pedido = whenInterpreta("Dame 1 kilo de yerba, aceite y 2 fideos");

        assertThat(pedido.lineas()).containsExactly(linea("yerba", "1"), linea("aceite", null), linea("fideos", "2"));
        assertThat(pedido.consumo()).isNull();
    }

    @Test
    @DisplayName("Entiende las cantidades en palabras y con decimales")
    void interpretarShouldReadQuantitiesInWords() {
        assertThat(whenInterpreta("un queso + medio kilo de jamón; 2,5 litros de leche; tres gaseosas").lineas())
                .containsExactly(linea("queso", "1"), linea("jamon", "0.5"), linea("leche", "2.5"),
                        linea("gaseosas", "3"));
    }

    @Test
    @DisplayName("Una palabra que sólo empieza como una cantidad es parte del producto")
    void interpretarShouldNotConfuseProductsStartingLikeAQuantity() {
        assertThat(whenInterpreta("unidades de leche, uno").lineas())
                .containsExactly(linea("leche", null), linea("uno", null));
    }

    @Test
    @DisplayName("Quita el saludo y la cortesía del principio, y la unidad que acompaña a la cantidad")
    void interpretarShouldDropTheGreeting() {
        assertThat(whenInterpreta("Hola, quiero harina").lineas()).containsExactly(linea("harina", null));
        assertThat(whenInterpreta("necesito 3 paquetes de fideos").lineas()).containsExactly(linea("fideos", "3"));
    }

    @Test
    @DisplayName("Un mensaje vacío no tiene líneas")
    void interpretarShouldReturnNoLinesForBlankText() {
        assertThat(whenInterpreta("   ").lineas()).isEmpty();
    }

    private PedidoInterpretado whenInterpreta(String texto) {
        return interprete.interpretar(new ConsultaPedido(texto, List.of()));
    }

    private static LineaPedida linea(String producto, String cantidad) {
        return new LineaPedida(producto, cantidad == null ? null : new BigDecimal(cantidad));
    }
}
