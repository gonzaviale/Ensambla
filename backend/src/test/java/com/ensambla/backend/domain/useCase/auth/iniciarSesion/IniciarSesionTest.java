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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IniciarSesionTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final ContraseniaPort contraseniaPort = mock(ContraseniaPort.class);
    private final IniciarSesion iniciarSesion = new IniciarSesion(usuarioRepository, contraseniaPort);
    private UsuarioResponse sesion;
    private Throwable error;

    @Test
    @DisplayName("Autentica con email normalizado y contraseña correcta y devuelve la identidad")
    void autenticaConEmailNormalizadoYContraseniaCorrecta() {
        dadoUnaCuenta(EstadoUsuario.ACTIVO);
        dadoQueLaContraseniaCoincide();

        cuandoIniciaSesion();

        entoncesLaSesionEsDeLaPersona();
    }

    @Test
    @DisplayName("Rechaza un email inexistente sin mirar ninguna contraseña")
    void rechazaUnEmailInexistente() {
        dadoQueNoHayCuentaConEseEmail();

        cuandoIntentaIniciarSesion();

        entoncesSeRechazanLasCredenciales();
        entoncesNoSeComparoLaContrasenia();
    }

    @Test
    @DisplayName("Rechaza un usuario suspendido sin validar la contraseña")
    void rechazaUnUsuarioSuspendido() {
        dadoUnaCuenta(EstadoUsuario.SUSPENDIDO);

        cuandoIntentaIniciarSesion();

        entoncesSeRechazanLasCredenciales();
        entoncesNoSeComparoLaContrasenia();
    }

    @Test
    @DisplayName("Rechaza una contraseña incorrecta")
    void rechazaUnaContraseniaIncorrecta() {
        dadoUnaCuenta(EstadoUsuario.ACTIVO);
        dadoQueLaContraseniaNoCoincide();

        cuandoIntentaIniciarSesion();

        entoncesSeRechazanLasCredenciales();
    }

    private void dadoUnaCuenta(EstadoUsuario estado) {
        Usuario usuario = usuario();
        usuario.setEstado(estado);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
    }

    private void dadoQueNoHayCuentaConEseEmail() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
    }

    private void dadoQueLaContraseniaCoincide() {
        when(contraseniaPort.coincide(CONTRASENIA, HASH)).thenReturn(true);
    }

    private void dadoQueLaContraseniaNoCoincide() {
        when(contraseniaPort.coincide(CONTRASENIA, HASH)).thenReturn(false);
    }

    private void cuandoIniciaSesion() {
        sesion = iniciarSesion.execute(new IniciarSesionRequest(" PERSONA@EXAMPLE.COM ", CONTRASENIA));
    }

    private void cuandoIntentaIniciarSesion() {
        error = catchThrowable(this::cuandoIniciaSesion);
    }

    private void entoncesLaSesionEsDeLaPersona() {
        assertThat(sesion).isEqualTo(new UsuarioResponse(usuario().getId(), NOMBRE, EMAIL));
    }

    private void entoncesSeRechazanLasCredenciales() {
        assertThat(error).isInstanceOf(CredencialesInvalidasException.class);
    }

    private void entoncesNoSeComparoLaContrasenia() {
        verifyNoInteractions(contraseniaPort);
    }
}