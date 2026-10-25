package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.ContraseniaPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.domain.useCase.auth.iniciarSesion.IniciarSesion;
import com.ensambla.backend.domain.useCase.auth.obtenerSesion.ObtenerSesion;
import com.ensambla.backend.domain.useCase.auth.registrarUsuario.RegistrarUsuario;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AuthUseCaseConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RegistrarUsuario registrarUsuario(
            UsuarioRepository usuarioRepository,
            NegocioRepository negocioRepository,
            PlanRepository planRepository,
            SuscripcionRepository suscripcionRepository,
            ContraseniaPort contraseniaPort,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new RegistrarUsuario(usuarioRepository, negocioRepository, planRepository, suscripcionRepository,
                contraseniaPort, transaccionPort, clock);
    }

    @Bean
    public IniciarSesion iniciarSesion(UsuarioRepository usuarioRepository, ContraseniaPort contraseniaPort) {
        return new IniciarSesion(usuarioRepository, contraseniaPort);
    }

    @Bean
    public ObtenerSesion obtenerSesion(UsuarioRepository usuarioRepository) {
        return new ObtenerSesion(usuarioRepository);
    }
}

