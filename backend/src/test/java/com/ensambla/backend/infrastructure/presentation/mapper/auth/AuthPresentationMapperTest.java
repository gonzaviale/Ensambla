package com.ensambla.backend.infrastructure.presentation.mapper.auth;

import com.ensambla.backend.domain.dto.auth.IniciarSesionRequest;
import com.ensambla.backend.domain.mapper.auth.RegistrarUsuarioMapper;
import com.ensambla.backend.domain.dto.auth.RegistrarUsuarioRequest;
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

    @Test
    @DisplayName("Mapea el registro al dominio y recorta el email")
    void toRequestShouldMapRegistro() {
        RegistroRequest input = new RegistroRequest(NOMBRE, "Negocio", " " + EMAIL + " ", CONTRASENIA);

        RegistrarUsuarioRequest result = mapper.toRequest(input);

        assertThat(result.nombre()).isEqualTo(NOMBRE);
        assertThat(result.nombreNegocio()).isEqualTo("Negocio");
        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.contrasenia()).isEqualTo(CONTRASENIA);
    }

    @Test
    @DisplayName("Mapea el login al dominio sin modificar la contraseña")
    void toRequestShouldMapLogin() {
        LoginRequest input = new LoginRequest(" " + EMAIL + " ", CONTRASENIA);

        IniciarSesionRequest result = mapper.toRequest(input);

        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.contrasenia()).isEqualTo(CONTRASENIA);
    }

    @Test
    @DisplayName("Mapea únicamente los datos públicos del usuario")
    void toResponseShouldMapIdentity() {
        var input = new RegistrarUsuarioMapper().toResponse(usuario());

        UsuarioResponse result = mapper.toResponse(input);

        assertThat(result.id()).isEqualTo(USUARIO_ID);
        assertThat(result.nombre()).isEqualTo(NOMBRE);
        assertThat(result.email()).isEqualTo(EMAIL);
    }
}

