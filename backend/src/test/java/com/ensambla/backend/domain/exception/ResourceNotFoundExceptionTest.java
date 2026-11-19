package com.ensambla.backend.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    private ResourceNotFoundException excepcion;

    @Test
    @DisplayName("Conserva el mensaje recibido")
    void conservaElMensajeRecibido() {
        cuandoSeCreaConElMensaje("No existe");

        entoncesTieneElMensaje("No existe");
    }

    @Test
    @DisplayName("Arma el mensaje con el recurso, el campo y el valor")
    void armaElMensajeConElRecursoElCampoYElValor() {
        cuandoSeCreaConElRecursoElCampoYElValor();

        entoncesTieneElMensaje("No se encontró un proyecto con id 7");
    }

    // --- cuando ---

    private void cuandoSeCreaConElMensaje(String mensaje) {
        excepcion = new ResourceNotFoundException(mensaje);
    }

    private void cuandoSeCreaConElRecursoElCampoYElValor() {
        excepcion = new ResourceNotFoundException("proyecto", "id", 7);
    }

    // --- entonces ---

    private void entoncesTieneElMensaje(String mensaje) {
        assertThat(excepcion).hasMessage(mensaje);
    }
}
