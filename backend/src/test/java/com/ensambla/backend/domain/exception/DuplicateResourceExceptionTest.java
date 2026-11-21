package com.ensambla.backend.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateResourceExceptionTest {

    private DuplicateResourceException excepcion;

    @Test
    @DisplayName("Conserva el mensaje recibido")
    void conservaElMensajeRecibido() {
        cuandoSeCreaConElMensaje("Ya existe");

        entoncesTieneElMensaje("Ya existe");
    }

    @Test
    @DisplayName("Arma el mensaje con el recurso, el campo y el valor")
    void armaElMensajeConElRecursoElCampoYElValor() {
        cuandoSeCreaConElRecursoElCampoYElValor();

        entoncesTieneElMensaje("Ya existe un usuario con email a@b.com");
    }

    // --- cuando ---

    private void cuandoSeCreaConElMensaje(String mensaje) {
        excepcion = new DuplicateResourceException(mensaje);
    }

    private void cuandoSeCreaConElRecursoElCampoYElValor() {
        excepcion = new DuplicateResourceException("usuario", "email", "a@b.com");
    }

    // --- entonces ---

    private void entoncesTieneElMensaje(String mensaje) {
        assertThat(excepcion).hasMessage(mensaje);
    }
}
