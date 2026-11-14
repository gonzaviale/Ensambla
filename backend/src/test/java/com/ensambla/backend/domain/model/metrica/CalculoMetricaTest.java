package com.ensambla.backend.domain.model.metrica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculoMetricaTest {

    @Test
    @DisplayName("Sumar y promediar dan pesos y usan el importe; contar y las unidades dan cantidades")
    void formatoShouldTellMoneyFromCounts() {
        assertThat(CalculoMetrica.SUMA.formato()).isEqualTo(FormatoMetrica.MONEDA);
        assertThat(CalculoMetrica.PROMEDIO.usaImporte()).isTrue();
        assertThat(CalculoMetrica.CONTEO.formato()).isEqualTo(FormatoMetrica.NUMERO);
        assertThat(CalculoMetrica.UNIDADES.usaImporte()).isFalse();
    }

    @Test
    @DisplayName("Sólo los gastos no tienen unidades")
    void sirveParaShouldExcludeUnitsOfExpenses() {
        assertThat(CalculoMetrica.UNIDADES.sirvePara(FuenteMetrica.VENTAS)).isTrue();
        assertThat(CalculoMetrica.UNIDADES.sirvePara(FuenteMetrica.INVENTARIO)).isTrue();
        assertThat(CalculoMetrica.UNIDADES.sirvePara(FuenteMetrica.GASTOS)).isFalse();
        assertThat(CalculoMetrica.SUMA.sirvePara(FuenteMetrica.GASTOS)).isTrue();
    }
}
