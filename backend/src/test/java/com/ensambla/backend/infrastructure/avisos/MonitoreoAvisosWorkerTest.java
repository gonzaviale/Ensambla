package com.ensambla.backend.infrastructure.avisos;

import com.ensambla.backend.domain.useCase.aviso.EjecutarMotorAvisosUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MonitoreoAvisosWorkerTest {

    @Test
    @DisplayName("Cada ciclo corre el motor de avisos")
    void monitorearShouldRunEngine() {
        EjecutarMotorAvisosUseCase useCase = mock(EjecutarMotorAvisosUseCase.class);

        new MonitoreoAvisosWorker(useCase).monitorear();

        verify(useCase).execute();
    }
}
