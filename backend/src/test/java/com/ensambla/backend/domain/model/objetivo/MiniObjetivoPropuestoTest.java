package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.JUSTIFICACION_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.TITULO_ACCION;
import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;

class MiniObjetivoPropuestoTest {

    private static final int POSICION_MEDIDO = 2;
    private static final int POSICION_MANUAL = 3;

    private MiniObjetivoPropuesto propuesto;
    private MiniObjetivo mini;

    @Test
    @DisplayName("Uno medido lleva la métrica del catálogo y el texto del servidor")
    void medidoShouldCarryTheCatalogMetric() {
        whenSeProponeUnMedido();

        thenLlevaLaMetricaDelCatalogo();
    }

    @Test
    @DisplayName("Uno manual lleva la acción y si la redactó la IA")
    void manualShouldCarryTheAction() {
        whenSeProponeUnManual();

        thenLlevaLaAccionRedactadaPorLaIa();
    }

    @Test
    @DisplayName("Uno medido se vuelve un mini-objetivo medido en curso con los números del candidato")
    void aMiniObjetivoShouldBuildAMeasuredMini() {
        givenUnMedido();

        whenSeVuelveMiniObjetivo(POSICION_MEDIDO);

        thenElMiniEsMedidoConLosNumerosDelCandidato();
    }

    @Test
    @DisplayName("Uno manual se vuelve un mini-objetivo manual en curso sin números")
    void aMiniObjetivoShouldBuildAManualMini() {
        givenUnManual();

        whenSeVuelveMiniObjetivo(POSICION_MANUAL);

        thenElMiniEsManualSinNumeros();
    }

    // --- arrange ---
    private void givenUnMedido() {
        propuesto = MiniObjetivoPropuesto.medido(candidato());
    }

    private void givenUnManual() {
        propuesto = MiniObjetivoPropuesto.manual(accion(), true);
    }

    // --- act ---
    private void whenSeProponeUnMedido() {
        givenUnMedido();
    }

    private void whenSeProponeUnManual() {
        givenUnManual();
    }

    private void whenSeVuelveMiniObjetivo(int posicion) {
        mini = propuesto.aMiniObjetivo(objetivo(), posicion, SEMANA, AHORA);
    }

    // --- assert ---
    private void thenLlevaLaMetricaDelCatalogo() {
        assertThat(propuesto.modo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
        assertThat(propuesto.esMedido()).isTrue();
        assertThat(propuesto.esManual()).isFalse();
        assertThat(propuesto.candidato()).isEqualTo(candidato());
        assertThat(propuesto.accion()).isNull();
        assertThat(propuesto.textoConIa()).isFalse();
    }

    private void thenLlevaLaAccionRedactadaPorLaIa() {
        assertThat(propuesto.modo()).isEqualTo(ModoMiniObjetivo.MANUAL);
        assertThat(propuesto.esManual()).isTrue();
        assertThat(propuesto.esMedido()).isFalse();
        assertThat(propuesto.accion()).isEqualTo(accion());
        assertThat(propuesto.candidato()).isNull();
        assertThat(propuesto.textoConIa()).isTrue();
    }

    private void thenElMiniEsMedidoConLosNumerosDelCandidato() {
        assertThat(mini.esMedido()).isTrue();
        assertThat(mini.getObjetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(mini.getPosicion()).isEqualTo(POSICION_MEDIDO);
        assertThat(mini.getMeta()).isEqualTo(META_MINI);
        assertThat(mini.getAporteEstimado()).isEqualTo(APORTE);
        assertThat(mini.getTitulo()).isEqualTo(TITULO);
        assertThat(mini.getSemanaInicio()).isEqualTo(SEMANA);
    }

    private void thenElMiniEsManualSinNumeros() {
        assertThat(mini.esManual()).isTrue();
        assertThat(mini.getPosicion()).isEqualTo(POSICION_MANUAL);
        assertThat(mini.getTitulo()).isEqualTo(TITULO_ACCION);
        assertThat(mini.getJustificacion()).isEqualTo(JUSTIFICACION_ACCION);
        assertThat(mini.isTextoConIa()).isTrue();
        assertThat(mini.getMeta()).isNull();
        assertThat(mini.getTipo()).isNull();
    }

    // --- helpers ---
    private static AccionManual accion() {
        return new AccionManual(TITULO_ACCION, JUSTIFICACION_ACCION);
    }
}
