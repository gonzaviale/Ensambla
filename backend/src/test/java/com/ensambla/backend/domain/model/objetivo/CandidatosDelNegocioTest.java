package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CandidatosDelNegocioTest {

    private static final BigDecimal META = new BigDecimal("500000.00");

    @Test
    @DisplayName("Alcanza la meta cuando el resultado la iguala o la pasa")
    void alcanzaShouldBeTrueWhenTheResultReachesTheGoal() {
        assertThat(negocioConResultado("500000").alcanza(META)).isTrue();
        assertThat(negocioConResultado("620000").alcanza(META)).isTrue();
    }

    @Test
    @DisplayName("No alcanza la meta cuando el resultado queda por debajo")
    void alcanzaShouldBeFalseWhenTheResultIsBelowTheGoal() {
        assertThat(negocioConResultado("499999").alcanza(META)).isFalse();
    }

    // --- helpers ---
    private static CandidatosDelNegocio negocioConResultado(String resultado) {
        return new CandidatosDelNegocio(new BigDecimal(resultado), List.of());
    }
}
