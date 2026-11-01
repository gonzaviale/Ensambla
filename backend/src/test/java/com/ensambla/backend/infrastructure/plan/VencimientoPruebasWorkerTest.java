package com.ensambla.backend.infrastructure.plan;

import com.ensambla.backend.domain.useCase.plan.vencerPruebas.VencerPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class VencimientoPruebasWorkerTest {

    @Test
    @DisplayName("Cada ciclo ejecuta el vencimiento de pruebas")
    void vencerShouldRunExpirationUseCase() {
        VencerPruebas vencerPruebas = mock(VencerPruebas.class);

        new VencimientoPruebasWorker(vencerPruebas).vencer();

        verify(vencerPruebas).execute();
    }

    @Test
    @DisplayName("El vencimiento se agenda automáticamente por defecto")
    void contextShouldScheduleTrialExpirationByDefault() {
        VencerPruebas vencerPruebas = mock(VencerPruebas.class);
        ApplicationContextRunner runner = givenRunner(vencerPruebas)
                .withPropertyValues("ensambla.planes.pruebas.intervalo=100",
                        "ensambla.planes.pruebas.demora-inicial=10");

        runner.run(context -> {
            assertThat(context).hasSingleBean(VencimientoPruebasWorker.class);
            verify(vencerPruebas, timeout(3000).atLeastOnce()).execute();
        });
    }

    @Test
    @DisplayName("El worker puede deshabilitarse explícitamente en las pruebas")
    void contextShouldDisableWorkerWhenConfigured() {
        ApplicationContextRunner runner = givenRunner(mock(VencerPruebas.class))
                .withPropertyValues("ensambla.planes.pruebas.habilitado=false");

        runner.run(context -> assertThat(context).doesNotHaveBean(VencimientoPruebasWorker.class));
    }

    // --- arrange ---
    private ApplicationContextRunner givenRunner(VencerPruebas vencerPruebas) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VencimientoPruebasWorker.class))
                .withUserConfiguration(Programacion.class)
                .withBean(VencerPruebas.class, () -> vencerPruebas);
    }

    @Configuration
    @EnableScheduling
    static class Programacion {
    }
}
