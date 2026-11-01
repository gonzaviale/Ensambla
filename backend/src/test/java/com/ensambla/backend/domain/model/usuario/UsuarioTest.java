package com.ensambla.backend.domain.model.usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    @Test
    @DisplayName("Un usuario ACTIVO está activo")
    void estaActivoShouldBeTrueWhenEstadoIsActivo() {
        Usuario usuario = Usuario.builder().estado(EstadoUsuario.ACTIVO).build();

        assertThat(usuario.estaActivo()).isTrue();
    }

    @Test
    @DisplayName("Un usuario SUSPENDIDO no está activo")
    void estaActivoShouldBeFalseWhenEstadoIsSuspendido() {
        Usuario usuario = Usuario.builder().estado(EstadoUsuario.SUSPENDIDO).build();

        assertThat(usuario.estaActivo()).isFalse();
    }

    @Test
    @DisplayName("Un usuario sin estado no está activo")
    void estaActivoShouldBeFalseWhenEstadoIsNull() {
        assertThat(new Usuario().estaActivo()).isFalse();
    }

    @Test
    @DisplayName("Normaliza el email recortando espacios y pasando a minúsculas")
    void normalizarEmailShouldTrimAndLowerCase() {
        String result = Usuario.normalizarEmail("  Persona@Example.COM ");

        assertThat(result).isEqualTo("persona@example.com");
    }
}
