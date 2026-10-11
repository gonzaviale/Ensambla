package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.useCase.procesamiento.ProcesarLoteUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProcesamientoIaWorkerTest {

    @Test
    @DisplayName("Un ciclo delega en el caso de uso del procesamiento")
    void procesarShouldInvokeUseCase() {
        ProcesarLoteUseCase useCase = mock(ProcesarLoteUseCase.class);
        ProcesamientoIaWorker worker = new ProcesamientoIaWorker(useCase);

        worker.procesar();

        verify(useCase).execute();
    }

    @Test
    @DisplayName("Un fallo no detiene los ciclos futuros del worker")
    void procesarShouldPreserveSchedulerAfterFailure() {
        ProcesarLoteUseCase useCase = mock(ProcesarLoteUseCase.class);
        doThrow(new IllegalStateException("Base no disponible")).when(useCase).execute();
        ProcesamientoIaWorker worker = new ProcesamientoIaWorker(useCase);

        assertThatCode(worker::procesar).doesNotThrowAnyException();
    }
}
