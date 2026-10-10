package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.infrastructure.ia.IaProperties;
import com.ensambla.backend.infrastructure.ia.ProcesamientoIaProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LecturaUseCaseConfigTest {

    @ParameterizedTest
    @CsvSource({"0,120,30", "-1,120,30", "60,60,30", "60,120,-1"})
    @DisplayName("Rechaza timeouts, reclamos y demoras inválidos")
    void politicaShouldRejectUnsafeDurations(int timeout, int vigencia, int demora) {
        var properties = new ProcesamientoIaProperties(3, 100, Duration.ofSeconds(demora),
                Duration.ofSeconds(vigencia), "privadas", null, null);
        var ia = new IaProperties("simulado", "", "", Duration.ofSeconds(timeout), 10, 0.8);
        var config = new LecturaUseCaseConfig();

        assertThatThrownBy(() -> config.politicaProcesamiento(properties, ia))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Timeout positivo");
    }
}
