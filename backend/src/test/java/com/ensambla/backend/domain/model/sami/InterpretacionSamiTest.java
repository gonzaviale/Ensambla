package com.ensambla.backend.domain.model.sami;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterpretacionSamiTest {

    @Test
    @DisplayName("Alcanza cuando hay plantilla y la confianza llega al umbral")
    void alcanzaShouldBeTrueWhenConfidenceReachesThreshold() {
        assertThat(interpretacion("Ventas", 0.7).alcanza(0.7)).isTrue();
    }

    @Test
    @DisplayName("No alcanza cuando la confianza queda debajo del umbral")
    void alcanzaShouldBeFalseWhenConfidenceIsBelowThreshold() {
        assertThat(interpretacion("Ventas", 0.69).alcanza(0.7)).isFalse();
    }

    @Test
    @DisplayName("No alcanza si no hay plantilla, aunque el umbral sea cero")
    void alcanzaShouldBeFalseWithoutTemplate() {
        assertThat(interpretacion(null, 0.0).alcanza(0.0)).isFalse();
    }

    private InterpretacionSami interpretacion(String plantilla, double confianza) {
        return new InterpretacionSami(null, plantilla, List.of(), confianza, List.of());
    }
}
