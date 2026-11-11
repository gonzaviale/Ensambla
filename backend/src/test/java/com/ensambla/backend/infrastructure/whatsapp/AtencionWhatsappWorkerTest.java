package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.useCase.pedido.atenderMensajeCliente.AtenderMensajeCliente;
import com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp.AtenderMensajeWhatsapp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtencionWhatsappWorkerTest {

    private final AtenderMensajeWhatsapp atender = mock(AtenderMensajeWhatsapp.class);
    private final AtenderMensajeCliente atenderCliente = mock(AtenderMensajeCliente.class);
    private final AtencionWhatsappWorker worker = new AtencionWhatsappWorker(atender, atenderCliente);

    @Test
    @DisplayName("Atiende mensajes mientras haya y se detiene cuando la cola queda vacía")
    void atenderShouldDrainTheQueue() {
        when(atender.execute()).thenReturn(true, true, false);

        worker.atender();

        verify(atender, times(3)).execute();
    }

    @Test
    @DisplayName("Una ráfaga se corta a los 20 mensajes por vuelta y el resto espera a la próxima")
    void atenderShouldCapTheBatch() {
        when(atender.execute()).thenReturn(true);

        worker.atender();

        verify(atender, times(AtencionWhatsappWorker.MAX_POR_VUELTA)).execute();
    }

    @Test
    @DisplayName("Un fallo no detiene las vueltas futuras del worker")
    void atenderShouldSurviveFailures() {
        doThrow(new IllegalStateException("Base no disponible")).when(atender).execute();

        assertThatCode(worker::atender).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("También vacía la cola de clientes con el mismo tope por vuelta")
    void atenderShouldDrainTheClientQueue() {
        when(atenderCliente.execute()).thenReturn(true, true, false);

        worker.atender();

        verify(atenderCliente, times(3)).execute();
    }

    @Test
    @DisplayName("Si falla la cola de comercios igual atiende la de clientes")
    void atenderShouldServeClientsWhenBusinessQueueFails() {
        doThrow(new IllegalStateException("Base no disponible")).when(atender).execute();

        worker.atender();

        verify(atenderCliente).execute();
    }

    @Test
    @DisplayName("Si falla la cola de clientes no rompe la vuelta")
    void atenderShouldSurviveClientQueueFailures() {
        doThrow(new IllegalStateException("Base no disponible")).when(atenderCliente).execute();

        assertThatCode(worker::atender).doesNotThrowAnyException();
    }
}
