package com.ensambla.backend.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("Conserva el mensaje recibido")
    void constructorShouldKeepMessage() {
        assertThat(new ResourceNotFoundException("No existe")).hasMessage("No existe");
    }

    @Test
    @DisplayName("Arma el mensaje con el recurso, el campo y el valor")
    void constructorShouldBuildMessageFromResourceFieldAndValue() {
        ResourceNotFoundException exception = new ResourceNotFoundException("proyecto", "id", 7);

        assertThat(exception).hasMessage("No se encontró un proyecto con id 7");
    }
}
