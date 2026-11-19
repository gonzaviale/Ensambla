package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.decision;
import static org.assertj.core.api.Assertions.assertThat;

class ConteoDecisionesTest {

    private List<DecisionObjetivo> decisiones;
    private ConteoDecisiones conteo;

    @Test
    @DisplayName("Cuenta las decisiones de cada clase")
    void cuentaLasDecisionesDeCadaClase() {
        dadoDecisionesDeAvisosYDePasos();

        cuandoSeCuentan();

        entoncesSeCuentaCadaClaseDeDecision();
    }

    @Test
    @DisplayName("Sin decisiones todo da cero")
    void sinDecisionesTodoDaCero() {
        dadoQueNoHayDecisiones();

        cuandoSeCuentan();

        entoncesElTotalEsCero();
    }

    // --- dado ---

    private void dadoDecisionesDeAvisosYDePasos() {
        decisiones = List.of(
                decision(TipoDecision.AVISO_APLICADO, AVISO_ID),
                decision(TipoDecision.AVISO_APLICADO, AVISO_ID),
                decision(TipoDecision.AVISO_DESCARTADO, AVISO_ID),
                decision(TipoDecision.PASO_ACEPTADO, null),
                decision(TipoDecision.PASO_IGNORADO, null));
    }

    private void dadoQueNoHayDecisiones() {
        decisiones = List.of();
    }

    // --- cuando ---

    private void cuandoSeCuentan() {
        conteo = ConteoDecisiones.de(decisiones);
    }

    // --- entonces ---

    private void entoncesSeCuentaCadaClaseDeDecision() {
        assertThat(conteo.aplicadas()).isEqualTo(2);
        assertThat(conteo.descartadas()).isEqualTo(1);
        assertThat(conteo.aceptadas()).isEqualTo(1);
        assertThat(conteo.ignoradas()).isEqualTo(1);
        assertThat(conteo.total()).isEqualTo(5);
    }

    private void entoncesElTotalEsCero() {
        assertThat(conteo.total()).isZero();
    }
}
