package com.ensambla.backend.domain.repository.usuario;

import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class UsuarioRepositoryTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("Devuelve el usuario cuando existe y está activo")
    void obtenerActivoShouldReturnUsuarioWhenActive() {
        givenRepositoryHas(Optional.of(usuario()));

        Usuario result = obtenerActivo();

        assertThat(result.getId()).isEqualTo(USUARIO_ID);
    }

    @Test
    @DisplayName("Rechaza al usuario suspendido con credenciales inválidas")
    void obtenerActivoShouldThrowWhenSuspended() {
        givenRepositoryHas(Optional.of(Usuario.builder().id(USUARIO_ID).estado(EstadoUsuario.SUSPENDIDO).build()));

        assertThatThrownBy(this::obtenerActivo).isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    @DisplayName("Rechaza al usuario inexistente con credenciales inválidas")
    void obtenerActivoShouldThrowWhenMissing() {
        givenRepositoryHas(Optional.empty());

        assertThatThrownBy(this::obtenerActivo).isInstanceOf(CredencialesInvalidasException.class);
    }

    // --- arrange ---
    private void givenRepositoryHas(Optional<Usuario> usuario) {
        doReturn(usuario).when(usuarioRepository).findById(USUARIO_ID);
    }

    // --- act ---
    private Usuario obtenerActivo() {
        return usuarioRepository.obtenerActivo(USUARIO_ID);
    }
}
