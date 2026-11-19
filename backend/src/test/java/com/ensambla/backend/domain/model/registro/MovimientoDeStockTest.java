package com.ensambla.backend.domain.model.registro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MovimientoDeStockTest {

    private MovimientoDeStock movimiento;

    @Test
    @DisplayName("Una entrada suma las unidades al stock")
    void unaEntradaSumaLasUnidadesAlStock() {
        cuandoSeAplica("5", "10", 1);

        entoncesElStockQuedaEn("15");
        entoncesElStockAlcanzo();
    }

    @Test
    @DisplayName("Una venta descuenta las unidades del stock")
    void unaVentaDescuentaLasUnidadesDelStock() {
        cuandoSeAplica("5", "5", -1);

        entoncesElStockQuedaEn("0");
        entoncesElStockAlcanzo();
    }

    @Test
    @DisplayName("Una venta de más unidades que las que hay deja el stock en 0 y avisa que no alcanzó")
    void unaVentaDeMasUnidadesQueLasQueHayDejaElStockEnCeroYAvisaQueNoAlcanzo() {
        cuandoSeAplica("1", "3", -1);

        entoncesElStockQuedaEn("0");
        entoncesElMovimientoPartioDeUnoYPidioTres();
        entoncesElStockNoAlcanzo();
    }

    // --- cuando ---
    private void cuandoSeAplica(String antes, String pedido, int signo) {
        movimiento = MovimientoDeStock.aplicar("Leche", new BigDecimal(antes), new BigDecimal(pedido), signo);
    }

    // --- entonces ---
    private void entoncesElStockQuedaEn(String esperado) {
        assertThat(movimiento.despues()).isEqualTo(new BigDecimal(esperado));
    }

    private void entoncesElStockAlcanzo() {
        assertThat(movimiento.noAlcanzo()).isFalse();
    }

    private void entoncesElStockNoAlcanzo() {
        assertThat(movimiento.noAlcanzo()).isTrue();
    }

    private void entoncesElMovimientoPartioDeUnoYPidioTres() {
        assertThat(movimiento.antes()).isEqualTo(new BigDecimal("1"));
        assertThat(movimiento.pedido()).isEqualTo(new BigDecimal("3"));
    }
}
