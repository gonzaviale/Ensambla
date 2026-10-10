package com.ensambla.backend.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CredencialesInvalidasExceptionTest {

    @Test
    @DisplayName("El error de acceso usa mensaje genérico y no contiene credenciales")
    void constructorShouldUseGenericMessage() {
        CredencialesInvalidasException exception = new CredencialesInvalidasException();

        thenConstructorShouldUseGenericMessage(exception);
    }

    // --- assert ---
    private void thenConstructorShouldUseGenericMessage(CredencialesInvalidasException exception) {
        assertThat(exception).isInstanceOf(DomainException.class)
                .hasMessage("No se pudo autenticar el acceso").hasNoCause();
    }
}
