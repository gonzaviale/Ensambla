package com.ensambla.backend.infrastructure.objetivos;

import com.ensambla.backend.domain.useCase.objetivo.monitorearObjetivos.MonitorearObjetivos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MonitoreoObjetivosWorkerTest {

    private final MonitorearObjetivos monitorearObjetivos = mock(MonitorearObjetivos.class);

    @Test
    @DisplayName("Cada ciclo corre el monitoreo de objetivos")
    void cadaCicloCorreElMonitoreoDeObjetivos() {
        cuandoCorreUnCicloDelMonitoreo();

        entoncesSeMonitoreanLosObjetivos();
    }

    // --- cuando ---
    private void cuandoCorreUnCicloDelMonitoreo() {
        new MonitoreoObjetivosWorker(monitorearObjetivos).monitorear();
    }

    // --- entonces ---
    private void entoncesSeMonitoreanLosObjetivos() {
        verify(monitorearObjetivos).execute();
    }
}
