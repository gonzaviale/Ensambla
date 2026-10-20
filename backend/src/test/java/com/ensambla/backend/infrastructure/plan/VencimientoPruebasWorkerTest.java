package com.ensambla.backend.infrastructure.plan;

import com.ensambla.backend.domain.useCase.plan.VencerPruebasUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class VencimientoPruebasWorkerTest {

    @Test
    @DisplayName("Cada ciclo ejecuta el vencimiento de pruebas")
    void vencerShouldRunExpirationUseCase() {
        VencerPruebasUseCase vencerPruebasUseCase = mock(VencerPruebasUseCase.class);

        new VencimientoPruebasWorker(vencerPruebasUseCase).vencer();

        verify(vencerPruebasUseCase).execute();
    }

    @Test
    @DisplayName("El vencimiento se agenda automáticamente por defecto")
    void contextShouldScheduleTrialExpirationByDefault() {
        VencerPruebasUseCase vencerPruebasUseCase = mock(VencerPruebasUseCase.class);
        ApplicationContextRunner runner = givenRunner(vencerPruebasUseCase)
                .withPropertyValues("ensambla.planes.pruebas.intervalo=100",
                        "ensambla.planes.pruebas.demora-inicial=10");

        runner.run(context -> {
            assertThat(context).hasSingleBean(VencimientoPruebasWorker.class);
            verify(vencerPruebasUseCase, timeout(3000).atLeastOnce()).execute();
        });
    }

    @Test
    @DisplayName("El worker puede deshabilitarse explícitamente en las pruebas")
    void contextShouldDisableWorkerWhenConfigured() {
        ApplicationContextRunner runner = givenRunner(mock(VencerPruebasUseCase.class))
                .withPropertyValues("ensambla.planes.pruebas.habilitado=false");

        runner.run(context -> assertThat(context).doesNotHaveBean(VencimientoPruebasWorker.class));
    }

    // --- arrange ---
    private ApplicationContextRunner givenRunner(VencerPruebasUseCase vencerPruebasUseCase) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VencimientoPruebasWorker.class))
                .withUserConfiguration(Programacion.class)
                .withBean(VencerPruebasUseCase.class, () -> vencerPruebasUseCase);
    }

    @Configuration
    @EnableScheduling
    static class Programacion {
    }
}
