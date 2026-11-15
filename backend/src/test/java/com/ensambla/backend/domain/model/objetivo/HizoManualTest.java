package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class HizoManualTest {

    private HizoManual respuesta;
    private TipoDecision decision;

    @ParameterizedTest
    @CsvSource({
            "SI, MINI_HECHO",
            "EN_PARTE, MINI_EN_PARTE",
            "NO, MINI_NO_HECHO"})
    @DisplayName("Cada respuesta deja su decisión en el historial del objetivo")
    void tipoDecisionShouldMapEveryAnswer(HizoManual hizo, TipoDecision esperada) {
        givenUnaRespuesta(hizo);

        whenSeAnotaLaDecision();

        thenLaDecisionEs(esperada);
    }

    // --- arrange ---
    private void givenUnaRespuesta(HizoManual hizo) {
        respuesta = hizo;
    }

    // --- act ---
    private void whenSeAnotaLaDecision() {
        decision = respuesta.tipoDecision();
    }

    // --- assert ---
    private void thenLaDecisionEs(TipoDecision esperada) {
        assertThat(decision).isEqualTo(esperada);
    }
}
