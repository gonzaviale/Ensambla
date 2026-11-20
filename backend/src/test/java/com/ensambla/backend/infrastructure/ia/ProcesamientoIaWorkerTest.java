package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.useCase.procesamiento.procesarLote.ProcesarLote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProcesamientoIaWorkerTest {

    private final ProcesarLote useCase = mock(ProcesarLote.class);
    private final ProcesamientoIaWorker worker = new ProcesamientoIaWorker(useCase);

    private Throwable fallo;

    @Test
    @DisplayName("Un ciclo delega en el caso de uso del procesamiento")
    void unCicloDelegaEnElCasoDeUsoDelProcesamiento() {
        cuandoElWorkerProcesa();

        entoncesSeEjecutaElCasoDeUso();
    }

    @Test
    @DisplayName("Un fallo no detiene los ciclos futuros del worker")
    void unFalloNoDetieneLosCiclosFuturosDelWorker() {
        dadoQueLaBaseNoEstaDisponible();

        cuandoElWorkerProcesaYFalla();

        entoncesElFalloNoSePropaga();
    }

    // --- dado ---

    private void dadoQueLaBaseNoEstaDisponible() {
        doThrow(new IllegalStateException("Base no disponible")).when(useCase).execute();
    }

    // --- cuando ---

    private void cuandoElWorkerProcesa() {
        worker.procesar();
    }

    private void cuandoElWorkerProcesaYFalla() {
        fallo = catchThrowable(worker::procesar);
    }

    // --- entonces ---

    private void entoncesSeEjecutaElCasoDeUso() {
        verify(useCase).execute();
    }

    private void entoncesElFalloNoSePropaga() {
        assertThat(fallo).isNull();
    }
}
