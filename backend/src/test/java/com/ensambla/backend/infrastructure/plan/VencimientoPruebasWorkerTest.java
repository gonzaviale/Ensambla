package com.ensambla.backend.infrastructure.plan;

import com.ensambla.backend.domain.useCase.plan.vencerPruebas.VencerPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VencimientoPruebasWorkerTest {

    private final VencerPruebas vencerPruebas = mock(VencerPruebas.class);
    private final CountDownLatch primerVencimiento = new CountDownLatch(1);
    private ApplicationContextRunner contexto;
    private int workersRegistrados;
    private boolean vencioAlMenosUnaVez;

    @Test
    @DisplayName("Cada ciclo ejecuta el vencimiento de pruebas")
    void cadaCicloEjecutaElVencimientoDePruebas() {
        cuandoCorreUnCicloDelWorker();

        entoncesSeVencenLasPruebas();
    }

    @Test
    @DisplayName("El vencimiento se agenda automáticamente por defecto")
    void elVencimientoSeAgendaAutomaticamentePorDefecto() {
        dadoUnWorkerQueVenceCadaCienMilisegundos();

        cuandoSeArrancaElContextoYSeEsperaElPrimerVencimiento();

        entoncesElWorkerSeRegistraYVenceAlMenosUnaVez();
    }

    @Test
    @DisplayName("El worker puede deshabilitarse explícitamente en las pruebas")
    void elWorkerPuedeDeshabilitarseExplicitamenteEnLasPruebas() {
        dadoUnWorkerDeshabilitado();

        cuandoSeArrancaElContexto();

        entoncesNoSeRegistraElWorker();
    }

    // --- dado ---
    private void dadoUnWorkerQueVenceCadaCienMilisegundos() {
        doAnswer(invocacion -> {
            primerVencimiento.countDown();
            return null;
        }).when(vencerPruebas).execute();
        contexto = dadoRunner().withPropertyValues("ensambla.planes.pruebas.intervalo=100",
                "ensambla.planes.pruebas.demora-inicial=10");
    }

    private void dadoUnWorkerDeshabilitado() {
        contexto = dadoRunner().withPropertyValues("ensambla.planes.pruebas.habilitado=false");
    }

    private ApplicationContextRunner dadoRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VencimientoPruebasWorker.class))
                .withUserConfiguration(Programacion.class)
                .withBean(VencerPruebas.class, () -> vencerPruebas);
    }

    // --- cuando ---
    private void cuandoCorreUnCicloDelWorker() {
        new VencimientoPruebasWorker(vencerPruebas).vencer();
    }

    private void cuandoSeArrancaElContexto() {
        contexto.run(arrancado -> workersRegistrados = arrancado.getBeansOfType(VencimientoPruebasWorker.class).size());
    }

    private void cuandoSeArrancaElContextoYSeEsperaElPrimerVencimiento() {
        contexto.run(arrancado -> {
            workersRegistrados = arrancado.getBeansOfType(VencimientoPruebasWorker.class).size();
            vencioAlMenosUnaVez = primerVencimiento.await(3, TimeUnit.SECONDS);
        });
    }

    // --- entonces ---
    private void entoncesSeVencenLasPruebas() {
        verify(vencerPruebas).execute();
    }

    private void entoncesElWorkerSeRegistraYVenceAlMenosUnaVez() {
        assertThat(workersRegistrados).isEqualTo(1);
        assertThat(vencioAlMenosUnaVez).isTrue();
    }

    private void entoncesNoSeRegistraElWorker() {
        assertThat(workersRegistrados).isZero();
    }

    @Configuration
    @EnableScheduling
    static class Programacion {
    }
}
