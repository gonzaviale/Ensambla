package com.ensambla.backend.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CredencialesInvalidasExceptionTest {

    private CredencialesInvalidasException excepcion;

    @Test
    @DisplayName("El error de acceso usa mensaje genérico y no contiene credenciales")
    void elErrorDeAccesoUsaMensajeGenericoYNoContieneCredenciales() {
        cuandoSeCreaElErrorDeAcceso();

        entoncesUsaElMensajeGenericoSinCausa();
    }

    // --- cuando ---

    private void cuandoSeCreaElErrorDeAcceso() {
        excepcion = new CredencialesInvalidasException();
    }

    // --- entonces ---

    private void entoncesUsaElMensajeGenericoSinCausa() {
        assertThat(excepcion).isInstanceOf(DomainException.class)
                .hasMessage("No se pudo autenticar el acceso").hasNoCause();
    }
}
