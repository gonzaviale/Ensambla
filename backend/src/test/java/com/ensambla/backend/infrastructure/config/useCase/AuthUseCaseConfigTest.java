package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.service.auth.RegistroService;
import com.ensambla.backend.domain.service.usuario.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthUseCaseConfigTest {

    private final AuthUseCaseConfig config = new AuthUseCaseConfig();

    @Test
    @DisplayName("Configura un reloj UTC y el caso de registro")
    void registrarUsuarioUseCaseShouldWireDependencies() {
        RegistroService service = mock(RegistroService.class);

        var result = config.registrarUsuarioUseCase(service, config.registrarUsuarioMapper(), config.clock());

        assertThat(result).isNotNull();
        assertThat(config.clock().getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Configura el caso de inicio de sesión")
    void iniciarSesionUseCaseShouldWireDependencies() {
        UsuarioService service = mock(UsuarioService.class);

        var result = config.iniciarSesionUseCase(service, config.iniciarSesionMapper());

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Configura el caso de consulta de sesión")
    void obtenerSesionUseCaseShouldWireDependencies() {
        UsuarioService service = mock(UsuarioService.class);

        var result = config.obtenerSesionUseCase(service, config.obtenerSesionMapper());

        assertThat(result).isNotNull();
    }
}

