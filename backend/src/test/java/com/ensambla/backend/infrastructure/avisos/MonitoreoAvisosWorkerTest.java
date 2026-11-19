package com.ensambla.backend.infrastructure.avisos;

import com.ensambla.backend.domain.useCase.aviso.ejecutarMotorAvisos.EjecutarMotorAvisos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MonitoreoAvisosWorkerTest {

    private final EjecutarMotorAvisos ejecutarMotorAvisos = mock(EjecutarMotorAvisos.class);

    @Test
    @DisplayName("Cada ciclo corre el motor de avisos")
    void cadaCicloCorreElMotorDeAvisos() {
        cuandoCorreUnCicloDelMonitoreo();

        entoncesSeEjecutaElMotorDeAvisos();
    }

    // --- cuando ---
    private void cuandoCorreUnCicloDelMonitoreo() {
        new MonitoreoAvisosWorker(ejecutarMotorAvisos).monitorear();
    }

    // --- entonces ---
    private void entoncesSeEjecutaElMotorDeAvisos() {
        verify(ejecutarMotorAvisos).execute();
    }
}
