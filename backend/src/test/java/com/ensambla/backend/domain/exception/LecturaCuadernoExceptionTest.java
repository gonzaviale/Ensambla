package com.ensambla.backend.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LecturaCuadernoExceptionTest {

    private static final String MENSAJE = "No pudimos leer el cuaderno";

    private LecturaCuadernoException excepcion;
    private Throwable causa;

    @Test
    @DisplayName("Es una excepción de dominio con el mensaje recibido")
    void esUnaExcepcionDeDominioConElMensajeRecibido() {
        cuandoSeCreaConElMensaje();

        entoncesEsDeDominioConElMensajeSinCausa();
    }

    @Test
    @DisplayName("Conserva la causa técnica")
    void conservaLaCausaTecnica() {
        cuandoSeCreaConElMensajeYUnTimeout();

        entoncesConservaElMensajeYLaCausa();
    }

    // --- cuando ---

    private void cuandoSeCreaConElMensaje() {
        excepcion = new LecturaCuadernoException(MENSAJE);
    }

    private void cuandoSeCreaConElMensajeYUnTimeout() {
        causa = new IllegalStateException("timeout");
        excepcion = new LecturaCuadernoException(MENSAJE, causa);
    }

    // --- entonces ---

    private void entoncesEsDeDominioConElMensajeSinCausa() {
        assertThat(excepcion).isInstanceOf(DomainException.class).hasMessage(MENSAJE).hasNoCause();
    }

    private void entoncesConservaElMensajeYLaCausa() {
        assertThat(excepcion).hasMessage(MENSAJE).hasCause(causa);
    }
}
