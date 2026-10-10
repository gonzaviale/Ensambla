package com.ensambla.backend.domain.service.usuario;

import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.model.enums.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.port.ContraseniaPort;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ensambla.backend.support.UsuarioTestData.CONTRASENIA;
import static com.ensambla.backend.support.UsuarioTestData.EMAIL;
import static com.ensambla.backend.support.UsuarioTestData.HASH;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticarUsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ContraseniaPort contraseniaPort;
    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("Autentica con email normalizado y contraseña correcta")
    void autenticarShouldReturnActiveUsuarioWhenPasswordMatches() {
        givenUsuario(EstadoUsuario.ACTIVO);
        when(contraseniaPort.coincide(CONTRASENIA, HASH)).thenReturn(true);

        Usuario result = autenticar();

        assertThat(result.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("Rechaza un email inexistente")
    void autenticarShouldRejectUnknownEmail() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        thenCredentialsRejected();

        verifyNoInteractions(contraseniaPort);
    }

    @Test
    @DisplayName("Rechaza un usuario suspendido sin validar la contraseña")
    void autenticarShouldRejectSuspendedUsuario() {
        givenUsuario(EstadoUsuario.SUSPENDIDO);

        thenCredentialsRejected();

        verifyNoInteractions(contraseniaPort);
    }

    @Test
    @DisplayName("Rechaza una contraseña incorrecta")
    void autenticarShouldRejectWrongPassword() {
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
    private Usuario autenticar() {
        return usuarioService.autenticar(" PERSONA@EXAMPLE.COM ", CONTRASENIA);
    }

    // --- assert ---
    private void thenCredentialsRejected() {
        assertThatThrownBy(this::autenticar).isInstanceOf(CredencialesInvalidasException.class);
    }
}

