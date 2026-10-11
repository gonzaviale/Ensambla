package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaProcesamientoTest {

    @Test
    @DisplayName("La lectura simulada tiene costo cero")
    void costoShouldReturnZeroWhenSimulated() {
        PoliticaProcesamiento politica = ProcesamientoTestData.politica();

        BigDecimal costo = politica.costo(ProcesamientoTestData.lectura().consumo());

        assertThat(costo).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("El costo queda desconocido si falta la tarifa de entrada")
    void costoShouldReturnNullWhenInputPriceMissing() {
        PoliticaProcesamiento politica = givenPrices(null, BigDecimal.ONE);

        BigDecimal costo = politica.costo(givenConsumo());

        assertThat(costo).isNull();
    }

    @Test
    @DisplayName("El costo queda desconocido si falta la tarifa de salida")
    void costoShouldReturnNullWhenOutputPriceMissing() {
        PoliticaProcesamiento politica = givenPrices(BigDecimal.ONE, null);

        BigDecimal costo = politica.costo(givenConsumo());

        assertThat(costo).isNull();
    }

    @Test
    @DisplayName("Calcula el costo estimado con las tarifas por millón")
    void costoShouldCalculateConfiguredPrices() {
        PoliticaProcesamiento politica = givenPrices(new BigDecimal("2"), new BigDecimal("6"));

        BigDecimal costo = politica.costo(givenConsumo());

        assertThat(costo).isEqualByComparingTo("0.002600");
    }

    private PoliticaProcesamiento givenPrices(BigDecimal entrada, BigDecimal salida) {
        PoliticaProcesamiento base = ProcesamientoTestData.politica();
        return new PoliticaProcesamiento(base.maxIntentos(), base.maxPaginas(), base.limiteDiario(),
                base.vigenciaReclamo(), base.demoraReintento(), entrada, salida);
    }

    private ConsumoIa givenConsumo() {
        return new ConsumoIa("real", "modelo", "v1", 1, 1000, 100, 20);
    }
}
