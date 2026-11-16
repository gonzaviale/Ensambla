package com.ensambla.backend.infrastructure.cartel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnMemoriaTest {

    @Test
    @DisplayName("Devuelve lo que arma el trabajo cuando sale bien")
    void sinFallarShouldReturnTheResult() {
        assertThat(EnMemoria.sinFallar(() -> "listo")).isEqualTo("listo");
    }

    @Test
    @DisplayName("Convierte el error verificado del API en uno de ejecución con su causa")
    void sinFallarShouldWrapCheckedErrors() {
        assertThatThrownBy(() -> EnMemoria.sinFallar(() -> {
            throw new IOException("disco lleno");
        })).isInstanceOf(IllegalStateException.class).hasCauseInstanceOf(IOException.class);
    }
}
