package com.ensambla.backend.domain.model.usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    private Usuario usuario;

    @Test
    @DisplayName("Un usuario ACTIVO está activo")
    void unUsuarioActivoEstaActivo() {
        dadoUnUsuario(EstadoUsuario.ACTIVO);

        entoncesEstaActivo();
    }

    @Test
    @DisplayName("Un usuario SUSPENDIDO no está activo")
    void unUsuarioSuspendidoNoEstaActivo() {
        dadoUnUsuario(EstadoUsuario.SUSPENDIDO);

        entoncesNoEstaActivo();
    }

    @Test
    @DisplayName("Un usuario sin estado no está activo")
    void unUsuarioSinEstadoNoEstaActivo() {
        dadoUnUsuarioSinEstado();

        entoncesNoEstaActivo();
    }

    @Test
    @DisplayName("Normaliza el email recortando espacios y pasando a minúsculas")
    void normalizaElEmailRecortandoEspaciosYPasandoAMinusculas() {
        entoncesElEmailNormalizadoQueda("  Persona@Example.COM ", "persona@example.com");
    }

    private void dadoUnUsuario(EstadoUsuario estado) {
        usuario = Usuario.builder().estado(estado).build();
    }

    private void dadoUnUsuarioSinEstado() {
        usuario = new Usuario();
    }

    private void entoncesEstaActivo() {
        assertThat(usuario.estaActivo()).isTrue();
    }

    private void entoncesNoEstaActivo() {
        assertThat(usuario.estaActivo()).isFalse();
    }

    private static void entoncesElEmailNormalizadoQueda(String email, String esperado) {
        assertThat(Usuario.normalizarEmail(email)).isEqualTo(esperado);
    }
}
