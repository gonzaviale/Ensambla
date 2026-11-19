package com.ensambla.backend.infrastructure.presentation.mapper.auth;

import com.ensambla.backend.domain.useCase.auth.iniciarSesion.IniciarSesionRequest;
import com.ensambla.backend.domain.useCase.auth.registrarUsuario.RegistrarUsuarioRequest;
import com.ensambla.backend.infrastructure.presentation.dto.auth.LoginRequest;
import com.ensambla.backend.infrastructure.presentation.dto.auth.RegistroRequest;
import com.ensambla.backend.infrastructure.presentation.dto.auth.UsuarioResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.UsuarioTestData.CONTRASENIA;
import static com.ensambla.backend.support.UsuarioTestData.EMAIL;
import static com.ensambla.backend.support.UsuarioTestData.NOMBRE;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;

class AuthPresentationMapperTest {

    private final AuthPresentationMapper mapper = new AuthPresentationMapper();
    private RegistrarUsuarioRequest registro;
    private IniciarSesionRequest login;
    private UsuarioResponse usuario;

    @Test
    @DisplayName("Mapea el registro al dominio y recorta el email")
    void mapeaElRegistroYRecortaElEmail() {
        cuandoLlegaElRegistro(new RegistroRequest(NOMBRE, "Negocio", " " + EMAIL + " ", CONTRASENIA));

        entoncesElRegistroEs(new RegistrarUsuarioRequest(NOMBRE, "Negocio", EMAIL, CONTRASENIA));
    }

    @Test
    @DisplayName("Mapea el login al dominio sin modificar la contraseña")
    void mapeaElLoginSinModificarLaContrasenia() {
        cuandoLlegaElLogin(new LoginRequest(" " + EMAIL + " ", CONTRASENIA));

        entoncesElLoginEs(new IniciarSesionRequest(EMAIL, CONTRASENIA));
    }

    @Test
    @DisplayName("Mapea únicamente los datos públicos del usuario")
    void mapeaUnicamenteLosDatosPublicosDelUsuario() {
        cuandoSeRespondeConElUsuario();

        entoncesLaRespuestaEs(USUARIO_ID, NOMBRE, EMAIL);
    }

    private void cuandoLlegaElRegistro(RegistroRequest pedido) {
        registro = mapper.toRequest(pedido);
    }

    private void cuandoLlegaElLogin(LoginRequest pedido) {
        login = mapper.toRequest(pedido);
    }

    private void cuandoSeRespondeConElUsuario() {
        usuario = mapper.toResponse(com.ensambla.backend.domain.useCase.auth.UsuarioResponse.de(usuario()));
    }

    private void entoncesElRegistroEs(RegistrarUsuarioRequest esperado) {
        assertThat(registro).isEqualTo(esperado);
    }

    private void entoncesElLoginEs(IniciarSesionRequest esperado) {
        assertThat(login).isEqualTo(esperado);
    }

    private void entoncesLaRespuestaEs(Object id, String nombre, String email) {
        assertThat(usuario.id()).isEqualTo(id);
        assertThat(usuario.nombre()).isEqualTo(nombre);
        assertThat(usuario.email()).isEqualTo(email);
    }
}