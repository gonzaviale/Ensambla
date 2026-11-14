package com.ensambla.backend.domain.model.metrica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FuenteMetricaTest {

    @Test
    @DisplayName("Las ventas y los gastos tienen fecha; el inventario no")
    void tieneFechaShouldBeFalseOnlyForInventory() {
        assertThat(FuenteMetrica.VENTAS.tieneFecha()).isTrue();
        assertThat(FuenteMetrica.GASTOS.tieneFecha()).isTrue();
        assertThat(FuenteMetrica.INVENTARIO.tieneFecha()).isFalse();
    }
}
