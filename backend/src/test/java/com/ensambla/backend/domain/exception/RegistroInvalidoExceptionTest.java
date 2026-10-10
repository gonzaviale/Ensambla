package com.ensambla.backend.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroInvalidoExceptionTest {

    @Test
    @DisplayName("Es una regla de negocio con un mensaje por campo")
    void shouldBeBusinessRuleWithErrorsPerField() {
        RegistroInvalidoException exception = new RegistroInvalidoException(Map.of("campo-1", "Es obligatorio"));

        assertThat(exception).isInstanceOf(BusinessRuleException.class);
        assertThat(exception.getMessage()).isEqualTo("Revisá los datos marcados");
        assertThat(exception.getErrores()).containsEntry("campo-1", "Es obligatorio");
    }

    @Test
    @DisplayName("Conserva una copia de los errores aunque el mapa original cambie")
    void shouldKeepCopyOfErrors() {
        Map<String, String> errores = new HashMap<>();
        errores.put("campo-1", "Es obligatorio");

        RegistroInvalidoException exception = new RegistroInvalidoException(errores);
        errores.put("campo-2", "Otro");

        assertThat(exception.getErrores()).containsOnlyKeys("campo-1");
    }
}
