package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HizoManualTest {

    @Test
    @DisplayName("Cada respuesta deja su decisión en el historial del objetivo")
    void tipoDecisionShouldMapEveryAnswer() {
        assertThat(HizoManual.SI.tipoDecision()).isEqualTo(TipoDecision.MINI_HECHO);
        assertThat(HizoManual.EN_PARTE.tipoDecision()).isEqualTo(TipoDecision.MINI_EN_PARTE);
        assertThat(HizoManual.NO.tipoDecision()).isEqualTo(TipoDecision.MINI_NO_HECHO);
    }
}
