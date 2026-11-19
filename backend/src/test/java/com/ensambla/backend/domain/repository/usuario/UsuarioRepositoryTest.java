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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class UsuarioRepositoryTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class, CALLS_REAL_METHODS);
    private Usuario activo;
    private Throwable error;

    @Test
    @DisplayName("Devuelve el usuario cuando existe y está activo")
    void devuelveElUsuarioCuandoExisteYEstaActivo() {
        dadoQueLaBaseTiene(Optional.of(usuario()));

        cuandoSeBuscaElUsuarioActivo();

        entoncesSeEncontroElUsuario();
    }

    @Test
    @DisplayName("Rechaza al usuario suspendido con credenciales inválidas")
    void rechazaAlUsuarioSuspendido() {
        dadoQueLaBaseTiene(Optional.of(Usuario.builder().id(USUARIO_ID).estado(EstadoUsuario.SUSPENDIDO).build()));

        cuandoSeIntentaBuscarElUsuarioActivo();

        entoncesSeRechazanLasCredenciales();
    }

    @Test
    @DisplayName("Rechaza al usuario inexistente con credenciales inválidas")
    void rechazaAlUsuarioInexistente() {
        dadoQueLaBaseTiene(Optional.empty());

        cuandoSeIntentaBuscarElUsuarioActivo();

        entoncesSeRechazanLasCredenciales();
    }

    private void dadoQueLaBaseTiene(Optional<Usuario> usuario) {
        doReturn(usuario).when(usuarioRepository).findById(USUARIO_ID);
    }

    private void cuandoSeBuscaElUsuarioActivo() {
        activo = usuarioRepository.obtenerActivo(USUARIO_ID);
    }

    private void cuandoSeIntentaBuscarElUsuarioActivo() {
        error = catchThrowable(this::cuandoSeBuscaElUsuarioActivo);
    }

    private void entoncesSeEncontroElUsuario() {
        assertThat(activo.getId()).isEqualTo(USUARIO_ID);
    }

    private void entoncesSeRechazanLasCredenciales() {
        assertThat(error).isInstanceOf(CredencialesInvalidasException.class);
    }
}