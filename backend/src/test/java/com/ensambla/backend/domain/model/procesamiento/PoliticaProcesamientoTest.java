package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaProcesamientoTest {

    private BigDecimal costo;

    @Test
    @DisplayName("La lectura simulada tiene costo cero")
    void laLecturaSimuladaTieneCostoCero() {
        cuandoSeCalculaElCostoDeUnaLecturaSimulada();

        entoncesElCostoEsCero();
    }

    @Test
    @DisplayName("El costo queda desconocido si falta la tarifa de entrada")
    void elCostoQuedaDesconocidoSiFaltaLaTarifaDeEntrada() {
        cuandoSeCalculaElCostoConTarifas(null, BigDecimal.ONE);

        entoncesElCostoEsDesconocido();
    }

    @Test
    @DisplayName("El costo queda desconocido si falta la tarifa de salida")
    void elCostoQuedaDesconocidoSiFaltaLaTarifaDeSalida() {
        cuandoSeCalculaElCostoConTarifas(BigDecimal.ONE, null);

        entoncesElCostoEsDesconocido();
    }

    @Test
    @DisplayName("Calcula el costo estimado con las tarifas por millón")
    void calculaElCostoEstimadoConLasTarifasPorMillon() {
        cuandoSeCalculaElCostoConTarifas(new BigDecimal("2"), new BigDecimal("6"));

        entoncesElCostoEs("0.002600");
    }

    // --- cuando ---
    private void cuandoSeCalculaElCostoDeUnaLecturaSimulada() {
        costo = ProcesamientoTestData.politica().costo(ProcesamientoTestData.lectura().consumo());
    }

    private void cuandoSeCalculaElCostoConTarifas(BigDecimal entrada, BigDecimal salida) {
        PoliticaProcesamiento base = ProcesamientoTestData.politica();
        PoliticaProcesamiento politica = new PoliticaProcesamiento(base.maxIntentos(), base.maxPaginas(),
                base.limiteDiario(), base.vigenciaReclamo(), base.demoraReintento(), entrada, salida);
        costo = politica.costo(unConsumo());
    }

    private static ConsumoIa unConsumo() {
        return new ConsumoIa("real", "modelo", "v1", 1, 1000, 100, 20);
    }

    // --- entonces ---
    private void entoncesElCostoEsCero() {
        assertThat(costo).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private void entoncesElCostoEsDesconocido() {
        assertThat(costo).isNull();
    }

    private void entoncesElCostoEs(String esperado) {
        assertThat(costo).isEqualByComparingTo(esperado);
    }
}
