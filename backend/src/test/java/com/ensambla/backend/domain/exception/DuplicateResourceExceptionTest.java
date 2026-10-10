package com.ensambla.backend.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateResourceExceptionTest {

    @Test
    @DisplayName("Conserva el mensaje recibido")
    void constructorShouldKeepMessage() {
        assertThat(new DuplicateResourceException("Ya existe")).hasMessage("Ya existe");
    }

    @Test
    @DisplayName("Arma el mensaje con el recurso, el campo y el valor")
    void constructorShouldBuildMessageFromResourceFieldAndValue() {
        DuplicateResourceException exception = new DuplicateResourceException("usuario", "email", "a@b.com");

        assertThat(exception).hasMessage("Ya existe un usuario con email a@b.com");
    }
}
