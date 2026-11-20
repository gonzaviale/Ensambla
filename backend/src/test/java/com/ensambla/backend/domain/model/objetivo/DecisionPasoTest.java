package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionPasoTest {

    private TipoDecision tipo;

    @Test
    @DisplayName("Aceptar el paso es la decisión de paso aceptado")
    void aceptarElPasoEsLaDecisionDePasoAceptado() {
        cuandoSePideElTipoDe(DecisionPaso.ACEPTADO);

        entoncesElTipoEs(TipoDecision.PASO_ACEPTADO);
    }

    @Test
    @DisplayName("Ignorar el paso es la decisión de paso ignorado")
    void ignorarElPasoEsLaDecisionDePasoIgnorado() {
        cuandoSePideElTipoDe(DecisionPaso.IGNORADO);

        entoncesElTipoEs(TipoDecision.PASO_IGNORADO);
    }

    // --- cuando ---

    private void cuandoSePideElTipoDe(DecisionPaso decision) {
        tipo = decision.tipo();
    }

    // --- entonces ---

    private void entoncesElTipoEs(TipoDecision esperado) {
        assertThat(tipo).isEqualTo(esperado);
    }
}
