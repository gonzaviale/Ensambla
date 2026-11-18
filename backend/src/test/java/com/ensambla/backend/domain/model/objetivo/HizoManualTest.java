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
    void cadaRespuestaDejaSuDecisionEnElHistorialDelObjetivo(HizoManual hizo, TipoDecision esperada) {
        dadoUnaRespuesta(hizo);

        cuandoSeAnotaLaDecision();

        entoncesLaDecisionEs(esperada);
    }

    // --- arrange ---
    private void dadoUnaRespuesta(HizoManual hizo) {
        respuesta = hizo;
    }

    // --- act ---
    private void cuandoSeAnotaLaDecision() {
        decision = respuesta.tipoDecision();
    }

    // --- assert ---
    private void entoncesLaDecisionEs(TipoDecision esperada) {
        assertThat(decision).isEqualTo(esperada);
    }
}
