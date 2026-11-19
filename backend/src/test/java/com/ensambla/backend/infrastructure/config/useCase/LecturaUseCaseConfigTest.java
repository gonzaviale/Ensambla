package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.infrastructure.ia.IaProperties;
import com.ensambla.backend.infrastructure.ia.ProcesamientoIaProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class LecturaUseCaseConfigTest {

    private Throwable error;

    @ParameterizedTest
    @CsvSource({"0,120,30", "-1,120,30", "60,60,30", "60,120,-1"})
    @DisplayName("Rechaza timeouts, reclamos y demoras inválidos")
    void rechazaTimeoutsReclamosYDemorasInvalidos(int timeout, int vigencia, int demora) {
        cuandoSeArmaLaPoliticaCon(timeout, vigencia, demora);

        entoncesSeRechazaPorTiemposInseguros();
    }

    private void cuandoSeArmaLaPoliticaCon(int timeout, int vigencia, int demora) {
        var procesamiento = new ProcesamientoIaProperties(3, 100, Duration.ofSeconds(demora),
                Duration.ofSeconds(vigencia), "privadas", null, null);
        var ia = new IaProperties("simulado", "", Duration.ofSeconds(timeout), 10, 0.8, null);
        error = catchThrowable(() -> new LecturaUseCaseConfig().politicaProcesamiento(procesamiento, ia));
    }

    private void entoncesSeRechazaPorTiemposInseguros() {
        assertThat(error).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Timeout positivo");
    }
}