package com.ensambla.backend.domain.model.pedido;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.PedidoTestData.producto;
import static org.assertj.core.api.Assertions.assertThat;

class CambioDeMarcaTest {

    private static final List<LineaDeCompra> LISTA = List.of(
            LineaDeCompra.de(producto("Yerba Playadito 1 kg", "3500", "10"), BigDecimal.ONE, List.of()),
            LineaDeCompra.de(producto("Aceite Cocinero 900 ml", "2000", "3"), BigDecimal.ONE, List.of()));

    @ParameterizedTest
    @ValueSource(strings = {"cambiar 2", "Cambiar 2", " cambia 2 ", "cambio 2"})
    @DisplayName("Reconoce el pedido de cambiar un renglón por su número, en cualquier forma del verbo")
    void pedidoShouldRecognizeTheNumber(String texto) {
        assertThat(CambioDeMarca.pedido(texto)).contains("2");
    }

    @Test
    @DisplayName("Reconoce el pedido por el nombre del producto, sin el artículo y sin lo que viene después de «por»")
    void pedidoShouldRecognizeTheProductName() {
        assertThat(CambioDeMarca.pedido("cambiar el aceite")).contains("aceite");
        assertThat(CambioDeMarca.pedido("Cambiá la yerba por otra")).contains("yerba");
        assertThat(CambioDeMarca.pedido("cambiar la yerba por otra")).contains("yerba");
        assertThat(CambioDeMarca.pedido("cambiar de la yerba")).contains("yerba");
    }

    @ParameterizedTest
    @ValueSource(strings = {"dame yerba", "cambiar", "sí", "", "quiero cambiar el aceite"})
    @DisplayName("Un mensaje que no es «cambiar» seguido de algo no es un pedido de cambio")
    void pedidoShouldIgnoreOtherTexts(String texto) {
        assertThat(CambioDeMarca.pedido(texto)).isEmpty();
    }

    @Test
    @DisplayName("El número elige el renglón si existe, empezando en 1")
    void renglonShouldPickTheLineByNumber() {
        assertThat(CambioDeMarca.renglon("1", LISTA)).hasValue(1);
        assertThat(CambioDeMarca.renglon("2", LISTA)).hasValue(2);
        assertThat(CambioDeMarca.renglon("0", LISTA)).isEmpty();
        assertThat(CambioDeMarca.renglon("3", LISTA)).isEmpty();
    }

    @Test
    @DisplayName("El nombre elige el renglón aunque esté escrito a medias, y si no coincide ninguno no elige")
    void renglonShouldPickTheLineByName() {
        assertThat(CambioDeMarca.renglon("aceite", LISTA)).hasValue(2);
        assertThat(CambioDeMarca.renglon("yerba playadito", LISTA)).hasValue(1);
        assertThat(CambioDeMarca.renglon("fideos", LISTA)).isEmpty();
    }
}
