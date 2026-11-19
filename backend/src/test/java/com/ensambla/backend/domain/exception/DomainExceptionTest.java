package com.ensambla.backend.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    private static final String MENSAJE_DE_NEGOCIO = "Operación no disponible";
    private static final String DETALLE_TECNICO = "Detalle técnico";

    private DomainException excepcion;
    private Throwable causa;

    @Test
    @DisplayName("Conserva el mensaje y no tiene causa cuando se crea sin causa")
    void conservaElMensajeYNoTieneCausaCuandoSeCreaSinCausa() {
        cuandoSeCreaConElMensaje(MENSAJE_DE_NEGOCIO);

        entoncesTieneElMensajeYNingunaCausa();
    }

    @Test
    @DisplayName("Conserva la causa sin perder el mensaje de negocio")
    void conservaLaCausaSinPerderElMensajeDeNegocio() {
        cuandoSeCreaConElMensajeYUnaCausaTecnica();

        entoncesTieneElMensajeYLaCausa();
    }

    @Test
    @DisplayName("Es una excepción no verificada")
    void esUnaExcepcionNoVerificada() {
        cuandoSeCreaConElMensaje(MENSAJE_DE_NEGOCIO);

        entoncesEsUnaExcepcionNoVerificada();
    }

    @Test
    @DisplayName("Admite un mensaje nulo")
    void admiteUnMensajeNulo() {
        cuandoSeCreaConElMensaje(null);

        entoncesNoTieneMensaje();
    }

    // --- cuando ---

    private void cuandoSeCreaConElMensaje(String mensaje) {
        excepcion = new DomainException(mensaje) {
        };
    }

    private void cuandoSeCreaConElMensajeYUnaCausaTecnica() {
        causa = new IllegalStateException(DETALLE_TECNICO);
        excepcion = new DomainException(MENSAJE_DE_NEGOCIO, causa) {
        };
    }

    // --- entonces ---

    private void entoncesTieneElMensajeYNingunaCausa() {
        assertThat(excepcion).hasMessage(MENSAJE_DE_NEGOCIO).hasNoCause();
    }

    private void entoncesTieneElMensajeYLaCausa() {
        assertThat(excepcion).hasMessage(MENSAJE_DE_NEGOCIO).hasCause(causa);
    }

    private void entoncesEsUnaExcepcionNoVerificada() {
        assertThat(excepcion).isInstanceOf(RuntimeException.class);
    }

    private void entoncesNoTieneMensaje() {
        assertThat(excepcion.getMessage()).isNull();
    }
}
