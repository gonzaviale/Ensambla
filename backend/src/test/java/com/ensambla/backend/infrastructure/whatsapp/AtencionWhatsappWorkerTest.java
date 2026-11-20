package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp.AtenderMensajeWhatsapp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtencionWhatsappWorkerTest {

    private final AtenderMensajeWhatsapp atender = mock(AtenderMensajeWhatsapp.class);
    private final AtencionWhatsappWorker worker = new AtencionWhatsappWorker(atender);

    private Throwable fallo;

    @Test
    @DisplayName("Atiende mensajes mientras haya y se detiene cuando la cola queda vacía")
    void atiendeMensajesMientrasHayaYSeDetieneCuandoLaColaQuedaVacia() {
        dadoUnaColaConDosMensajes();

        cuandoElWorkerAtiende();

        entoncesSeAtiendeTresVecesHastaVaciarLaCola();
    }

    @Test
    @DisplayName("Una ráfaga se corta a los 20 mensajes por vuelta y el resto espera a la próxima")
    void unaRafagaSeCortaALos20MensajesPorVueltaYElRestoEsperaALaProxima() {
        dadoUnaColaSinFin();

        cuandoElWorkerAtiende();

        entoncesSeAtiendeElMaximoPorVuelta();
    }

    @Test
    @DisplayName("Un fallo no detiene las vueltas futuras del worker")
    void unFalloNoDetieneLasVueltasFuturasDelWorker() {
        dadoQueLaBaseNoEstaDisponible();

        cuandoElWorkerAtiendeYFalla();

        entoncesElFalloNoSePropaga();
    }

    // --- dado ---

    private void dadoUnaColaConDosMensajes() {
        when(atender.execute()).thenReturn(true, true, false);
    }

    private void dadoUnaColaSinFin() {
        when(atender.execute()).thenReturn(true);
    }

    private void dadoQueLaBaseNoEstaDisponible() {
        doThrow(new IllegalStateException("Base no disponible")).when(atender).execute();
    }

    // --- cuando ---

    private void cuandoElWorkerAtiende() {
        worker.atender();
    }

    private void cuandoElWorkerAtiendeYFalla() {
        fallo = catchThrowable(worker::atender);
    }

    // --- entonces ---

    private void entoncesSeAtiendeTresVecesHastaVaciarLaCola() {
        verify(atender, times(3)).execute();
    }

    private void entoncesSeAtiendeElMaximoPorVuelta() {
        verify(atender, times(AtencionWhatsappWorker.MAX_POR_VUELTA)).execute();
    }

    private void entoncesElFalloNoSePropaga() {
        assertThat(fallo).isNull();
    }
}
