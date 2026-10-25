package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.ContraseniaPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.support.TransaccionDirecta;
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
        var result = config.registrarUsuario(mock(UsuarioRepository.class), mock(NegocioRepository.class),
                mock(PlanRepository.class), mock(SuscripcionRepository.class), mock(ContraseniaPort.class),
                new TransaccionDirecta(), config.clock());

        assertThat(result).isNotNull();
        assertThat(config.clock().getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Configura el caso de inicio de sesión")
    void iniciarSesionUseCaseShouldWireDependencies() {
        var result = config.iniciarSesion(mock(UsuarioRepository.class), mock(ContraseniaPort.class));

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Configura el caso de consulta de sesión")
    void obtenerSesionUseCaseShouldWireDependencies() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

        var result = config.obtenerSesion(usuarioRepository);

        assertThat(result).isNotNull();
    }
}

