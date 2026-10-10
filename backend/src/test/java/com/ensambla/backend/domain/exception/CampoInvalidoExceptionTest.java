package com.ensambla.backend.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CampoInvalidoExceptionTest {

    @Test
    @DisplayName("Es una regla de negocio con un mensaje por dato de la configuración")
    void shouldBeBusinessRuleWithErrorsPerDato() {
        CampoInvalidoException exception = new CampoInvalidoException(Map.of("opciones", "Agregá al menos una opción"));

        assertThat(exception).isInstanceOf(BusinessRuleException.class);
        assertThat(exception.getMessage()).isEqualTo("Revisá la configuración del campo");
        assertThat(exception.getErrores()).containsEntry("opciones", "Agregá al menos una opción");
    }

    @Test
    @DisplayName("Conserva una copia de los errores aunque el mapa original cambie")
    void shouldKeepCopyOfErrors() {
        Map<String, String> errores = new HashMap<>();
        errores.put("tipo", "Elegí un tipo válido");

        CampoInvalidoException exception = new CampoInvalidoException(errores);
        errores.put("nombre", "Otro");

        assertThat(exception.getErrores()).containsOnlyKeys("tipo");
    }
}
