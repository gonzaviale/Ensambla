package com.ensambla.backend.domain.model.metrica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultadoMetricaTest {

    private static final DeDondeSale ORIGEN = new DeDondeSale(FuenteMetrica.VENTAS, 3, 1, null, null);

    @Test
    @DisplayName("Un resultado con valor es calculable y no trae motivo")
    void conValorShouldBeCalculable() {
        ResultadoMetrica resultado = ResultadoMetrica.conValor(
                FormatoMetrica.MONEDA, BigDecimal.TEN, List.of(new ResultadoMetrica.Fila("Pan", BigDecimal.ONE)),
                ORIGEN);

        assertThat(resultado.calculable()).isTrue();
        assertThat(resultado.valor()).isEqualTo(BigDecimal.TEN);
        assertThat(resultado.motivo()).isNull();
        assertThat(resultado.filas()).hasSize(1);
    }

    @Test
    @DisplayName("Un resultado sin valor explica el motivo y no tiene reparto")
    void sinValorShouldCarryTheReason() {
        ResultadoMetrica resultado = ResultadoMetrica.sinValor(FormatoMetrica.NUMERO, "No hay datos", ORIGEN);

        assertThat(resultado.calculable()).isFalse();
        assertThat(resultado.valor()).isNull();
        assertThat(resultado.motivo()).isEqualTo("No hay datos");
        assertThat(resultado.filas()).isEmpty();
    }

    @Test
    @DisplayName("Sin reparto la lista queda vacía")
    void constructorShouldDefaultFilasToEmpty() {
        ResultadoMetrica resultado = new ResultadoMetrica(FormatoMetrica.NUMERO, BigDecimal.ONE, null, null, ORIGEN);

        assertThat(resultado.filas()).isEmpty();
    }
}
