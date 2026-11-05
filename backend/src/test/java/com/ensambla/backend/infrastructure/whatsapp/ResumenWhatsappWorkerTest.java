package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.useCase.whatsapp.enviarResumenPorWhatsapp.EnviarResumenPorWhatsapp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumenWhatsappWorkerTest {

    private final EnviarResumenPorWhatsapp enviarResumen = mock(EnviarResumenPorWhatsapp.class);
    private final ResumenWhatsappWorker worker = new ResumenWhatsappWorker(enviarResumen);

    @Test
    @DisplayName("En cada vuelta le pide al caso de uso que mande los resúmenes que tocan")
    void enviarShouldRunTheUseCase() {
        when(enviarResumen.execute()).thenReturn(2);

        worker.enviar();

        verify(enviarResumen).execute();
    }

    @Test
    @DisplayName("Un fallo no detiene las vueltas futuras del worker")
    void enviarShouldSurviveFailures() {
        doThrow(new IllegalStateException("Base no disponible")).when(enviarResumen).execute();

        assertThatCode(worker::enviar).doesNotThrowAnyException();
    }
}
