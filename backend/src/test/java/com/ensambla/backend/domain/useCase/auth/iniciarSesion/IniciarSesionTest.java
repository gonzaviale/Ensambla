package com.ensambla.backend.domain.useCase.auth.iniciarSesion;

import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.port.ContraseniaPort;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.domain.useCase.auth.UsuarioResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.UsuarioTestData.CONTRASENIA;
import static com.ensambla.backend.support.UsuarioTestData.EMAIL;
import static com.ensambla.backend.support.UsuarioTestData.HASH;
import static com.ensambla.backend.support.UsuarioTestData.NOMBRE;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IniciarSesionTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final ContraseniaPort contraseniaPort = mock(ContraseniaPort.class);
    private final IniciarSesion iniciarSesion = new IniciarSesion(usuarioRepository, contraseniaPort);

    @Test
    @DisplayName("Autentica con email normalizado y contraseña correcta y devuelve la identidad")
    void executeShouldReturnIdentityWhenPasswordMatches() {
        givenUsuario(EstadoUsuario.ACTIVO);
        when(contraseniaPort.coincide(CONTRASENIA, HASH)).thenReturn(true);

        UsuarioResponse result = iniciarSesion();

        assertThat(result).isEqualTo(new UsuarioResponse(usuario().getId(), NOMBRE, EMAIL));
    }

    @Test
    @DisplayName("Rechaza un email inexistente")
    void executeShouldRejectUnknownEmail() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        thenCredentialsRejected();

        verifyNoInteractions(contraseniaPort);
    }

    @Test
    @DisplayName("Rechaza un usuario suspendido sin validar la contraseña")
    void executeShouldRejectSuspendedUsuario() {
        givenUsuario(EstadoUsuario.SUSPENDIDO);

        thenCredentialsRejected();

        verifyNoInteractions(contraseniaPort);
    }

    @Test
    @DisplayName("Rechaza una contraseña incorrecta")
    void executeShouldRejectWrongPassword() {
        givenUsuario(EstadoUsuario.ACTIVO);
        when(contraseniaPort.coincide(CONTRASENIA, HASH)).thenReturn(false);

        thenCredentialsRejected();
    }

    // --- arrange ---
    private void givenUsuario(EstadoUsuario estado) {
        Usuario usuario = usuario();
        usuario.setEstado(estado);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
    }

    // --- act ---
    private UsuarioResponse iniciarSesion() {
        return iniciarSesion.execute(new IniciarSesionRequest(" PERSONA@EXAMPLE.COM ", CONTRASENIA));
    }

    // --- assert ---
    private void thenCredentialsRejected() {
        assertThatThrownBy(this::iniciarSesion).isInstanceOf(CredencialesInvalidasException.class);
    }
}
