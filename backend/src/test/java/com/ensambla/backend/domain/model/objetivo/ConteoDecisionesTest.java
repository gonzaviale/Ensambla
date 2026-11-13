package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.decision;
import static org.assertj.core.api.Assertions.assertThat;

class ConteoDecisionesTest {

    @Test
    @DisplayName("Cuenta las decisiones de cada clase")
    void deShouldCountEachTypeOfDecision() {
        ConteoDecisiones result = ConteoDecisiones.de(List.of(
                decision(TipoDecision.AVISO_APLICADO, AVISO_ID),
                decision(TipoDecision.AVISO_APLICADO, AVISO_ID),
                decision(TipoDecision.AVISO_DESCARTADO, AVISO_ID),
                decision(TipoDecision.PASO_ACEPTADO, null),
                decision(TipoDecision.PASO_IGNORADO, null)));

        assertThat(result.aplicadas()).isEqualTo(2);
        assertThat(result.descartadas()).isEqualTo(1);
        assertThat(result.aceptadas()).isEqualTo(1);
        assertThat(result.ignoradas()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(5);
    }

    @Test
    @DisplayName("Sin decisiones todo da cero")
    void deShouldBeZeroWhenThereAreNoDecisions() {
        assertThat(ConteoDecisiones.de(List.of()).total()).isZero();
    }
}
