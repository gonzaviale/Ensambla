package com.ensambla.backend.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    private static final String BUSINESS_MESSAGE = "Operación no disponible";
    private static final String TECHNICAL_DETAIL = "Detalle técnico";

    @Test
    @DisplayName("Conserva el mensaje y no tiene causa cuando se crea sin causa")
    void constructorShouldKeepMessageWithoutCauseWhenCreatedWithMessageOnly() {
        DomainException exception = domainException(BUSINESS_MESSAGE);

        thenHasMessageAndNoCause(exception);
    }

    @Test
    @DisplayName("Conserva la causa sin perder el mensaje de negocio")
    void constructorShouldKeepCauseAndMessageWhenCreatedWithCause() {
        IllegalStateException cause = new IllegalStateException(TECHNICAL_DETAIL);

        DomainException exception = domainException(BUSINESS_MESSAGE, cause);

        thenHasMessageAndCause(exception, cause);
    }

    @Test
    @DisplayName("Es una excepción no verificada")
    void domainExceptionShouldBeRuntimeException() {
        DomainException exception = domainException(BUSINESS_MESSAGE);

        thenIsRuntimeException(exception);
    }

    @Test
    @DisplayName("Admite un mensaje nulo")
    void constructorShouldAllowNullMessageWhenMessageIsNull() {
        DomainException exception = domainException(null);

        thenHasNullMessage(exception);
    }

    // --- act ---
    private DomainException domainException(String message) {
        return new DomainException(message) {
        };
    }

    private DomainException domainException(String message, Throwable cause) {
        return new DomainException(message, cause) {
        };
    }

    // --- assert ---
    private void thenHasMessageAndNoCause(DomainException exception) {
        assertThat(exception).hasMessage(BUSINESS_MESSAGE).hasNoCause();
    }

    private void thenHasMessageAndCause(DomainException exception, Throwable cause) {
        assertThat(exception).hasMessage(BUSINESS_MESSAGE).hasCause(cause);
    }

    private void thenIsRuntimeException(DomainException exception) {
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    private void thenHasNullMessage(DomainException exception) {
        assertThat(exception.getMessage()).isNull();
    }
}
