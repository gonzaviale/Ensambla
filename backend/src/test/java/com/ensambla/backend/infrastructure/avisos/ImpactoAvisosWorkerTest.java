package com.ensambla.backend.infrastructure.avisos;

import com.ensambla.backend.domain.useCase.aviso.medirImpactoAvisos.MedirImpactoAvisos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ImpactoAvisosWorkerTest {

    @Test
    @DisplayName("Cada ciclo mide el impacto de los avisos aplicados")
    void medirShouldRunTheMeasurement() {
        MedirImpactoAvisos useCase = mock(MedirImpactoAvisos.class);

        new ImpactoAvisosWorker(useCase).medir();

        verify(useCase).execute();
    }
}
