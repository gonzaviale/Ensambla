package com.ensambla.backend.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LecturaCuadernoExceptionTest {

    private static final String MENSAJE = "No pudimos leer el cuaderno";

    @Test
    @DisplayName("Es una excepción de dominio con el mensaje recibido")
    void constructorShouldKeepMessage() {
        LecturaCuadernoException exception = new LecturaCuadernoException(MENSAJE);

        assertThat(exception).isInstanceOf(DomainException.class).hasMessage(MENSAJE).hasNoCause();
    }

    @Test
    @DisplayName("Conserva la causa técnica")
    void constructorShouldKeepCause() {
        IllegalStateException causa = new IllegalStateException("timeout");

        LecturaCuadernoException exception = new LecturaCuadernoException(MENSAJE, causa);

        assertThat(exception).hasMessage(MENSAJE).hasCause(causa);
    }
}
