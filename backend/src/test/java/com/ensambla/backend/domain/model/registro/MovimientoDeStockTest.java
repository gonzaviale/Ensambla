package com.ensambla.backend.domain.model.registro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MovimientoDeStockTest {

    @Test
    @DisplayName("Una entrada suma las unidades al stock")
    void aplicarShouldAddAnEntry() {
        MovimientoDeStock movimiento = MovimientoDeStock.aplicar("Leche", new BigDecimal("5"), new BigDecimal("10"), 1);

        assertThat(movimiento.despues()).isEqualTo(new BigDecimal("15"));
        assertThat(movimiento.noAlcanzo()).isFalse();
    }

    @Test
    @DisplayName("Una venta descuenta las unidades del stock")
    void aplicarShouldSubtractASale() {
        MovimientoDeStock movimiento = MovimientoDeStock.aplicar("Leche", new BigDecimal("5"), new BigDecimal("5"), -1);

        assertThat(movimiento.despues()).isEqualTo(BigDecimal.ZERO);
        assertThat(movimiento.noAlcanzo()).isFalse();
    }

    @Test
    @DisplayName("Una venta de más unidades que las que hay deja el stock en 0 y avisa que no alcanzó")
    void aplicarShouldStopAtZeroWhenTheStockIsShort() {
        MovimientoDeStock movimiento = MovimientoDeStock.aplicar("Leche", new BigDecimal("1"), new BigDecimal("3"), -1);

        assertThat(movimiento.despues()).isEqualTo(BigDecimal.ZERO);
        assertThat(movimiento.antes()).isEqualTo(new BigDecimal("1"));
        assertThat(movimiento.pedido()).isEqualTo(new BigDecimal("3"));
        assertThat(movimiento.noAlcanzo()).isTrue();
    }
}
