package com.ensambla.backend.infrastructure.avisos;

import com.ensambla.backend.domain.useCase.aviso.medirImpactoAvisos.MedirImpactoAvisos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ImpactoAvisosWorkerTest {

    private final MedirImpactoAvisos medirImpactoAvisos = mock(MedirImpactoAvisos.class);

    @Test
    @DisplayName("Cada ciclo mide el impacto de los avisos aplicados")
    void cadaCicloMideElImpactoDeLosAvisosAplicados() {
        cuandoCorreUnCicloDeMedicion();

        entoncesSeMideElImpactoDeLosAvisos();
    }

    // --- cuando ---
    private void cuandoCorreUnCicloDeMedicion() {
        new ImpactoAvisosWorker(medirImpactoAvisos).medir();
    }

    // --- entonces ---
    private void entoncesSeMideElImpactoDeLosAvisos() {
        verify(medirImpactoAvisos).execute();
    }
}
