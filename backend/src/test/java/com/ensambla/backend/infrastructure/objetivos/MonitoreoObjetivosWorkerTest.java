package com.ensambla.backend.infrastructure.objetivos;

import com.ensambla.backend.domain.useCase.objetivo.monitorearObjetivos.MonitorearObjetivos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MonitoreoObjetivosWorkerTest {

    @Test
    @DisplayName("Cada ciclo corre el monitoreo de objetivos")
    void monitorearShouldRunTheMonitoring() {
        MonitorearObjetivos useCase = mock(MonitorearObjetivos.class);

        new MonitoreoObjetivosWorker(useCase).monitorear();

        verify(useCase).execute();
    }
}
