package com.ensambla.backend.domain.model.reposicion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SituacionStockTest {

    private static final BigDecimal MINIMO = BigDecimal.TEN;

    @Test
    @DisplayName("Un producto sin unidades está agotado, aunque tenga mínimo")
    void detectarShouldReturnAgotadoWhenThereAreNoUnits() {
        Optional<SituacionStock> result = SituacionStock.detectar(BigDecimal.ZERO, MINIMO);

        thenSeDescribeComo(result, "Agotado");
    }

    @Test
    @DisplayName("Una cantidad negativa también es agotado")
    void detectarShouldReturnAgotadoWhenTheQuantityIsNegative() {
        Optional<SituacionStock> result = SituacionStock.detectar(new BigDecimal("-2"), null);

        thenSeDescribeComo(result, "Agotado");
    }

    @Test
    @DisplayName("Con unidades por debajo del mínimo está bajo el mínimo")
    void detectarShouldReturnBajoMinimoWhenBelowTheMinimum() {
        Optional<SituacionStock> result = SituacionStock.detectar(new BigDecimal("4"), MINIMO);

        thenSeDescribeComo(result, "Quedan 4 (mínimo 10)");
    }

    @Test
    @DisplayName("Con más unidades que el mínimo no hace falta reponer")
    void detectarShouldReturnEmptyWhenAboveTheMinimum() {
        Optional<SituacionStock> result = SituacionStock.detectar(new BigDecimal("11"), MINIMO);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Sin mínimo definido solo importa si se agotó")
    void detectarShouldReturnEmptyWhenThereIsNoMinimumAndThereAreUnits() {
        Optional<SituacionStock> result = SituacionStock.detectar(BigDecimal.ONE, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Un mínimo en cero no cuenta como mínimo")
    void detectarShouldIgnoreAZeroMinimum() {
        Optional<SituacionStock> result = SituacionStock.detectar(new BigDecimal("5"), BigDecimal.ZERO);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Pide lo que falta para llegar al doble del mínimo")
    void pedidoParaShouldReachDoubleTheMinimum() {
        BigDecimal result = SituacionStock.pedidoPara(new BigDecimal("3"), MINIMO);

        assertThat(result).isEqualByComparingTo("17");
    }

    @Test
    @DisplayName("Sin mínimo pide para llegar a diez")
    void pedidoParaShouldReachTenWhenThereIsNoMinimum() {
        BigDecimal result = SituacionStock.pedidoPara(new BigDecimal("4"), null);

        assertThat(result).isEqualByComparingTo("6");
    }

    @Test
    @DisplayName("Nunca pide menos de una unidad")
    void pedidoParaShouldAskForAtLeastOneUnit() {
        BigDecimal result = SituacionStock.pedidoPara(new BigDecimal("25"), MINIMO);

        assertThat(result).isEqualByComparingTo("1");
    }

    // --- assert ---
    private void thenSeDescribeComo(Optional<SituacionStock> situacion, String descripcion) {
        assertThat(situacion).hasValueSatisfying(valor -> assertThat(valor.descripcion()).isEqualTo(descripcion));
    }
}
