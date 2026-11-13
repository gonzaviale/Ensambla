package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionPasoTest {

    @Test
    @DisplayName("Aceptar el paso es la decisión de paso aceptado")
    void tipoShouldBeAcceptedForAcceptedStep() {
        assertThat(DecisionPaso.ACEPTADO.tipo()).isEqualTo(TipoDecision.PASO_ACEPTADO);
    }

    @Test
    @DisplayName("Ignorar el paso es la decisión de paso ignorado")
    void tipoShouldBeIgnoredForIgnoredStep() {
        assertThat(DecisionPaso.IGNORADO.tipo()).isEqualTo(TipoDecision.PASO_IGNORADO);
    }
}
