package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.ContraseniaPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.domain.service.auth.RegistroService;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.domain.service.usuario.UsuarioService;
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
            RegistroService registroService, Clock clock) {
        return new RegistrarUsuario(registroService, clock);
    }

    @Bean
    public IniciarSesion iniciarSesion(
            UsuarioService usuarioService) {
        return new IniciarSesion(usuarioService);
    }

    @Bean
    public ObtenerSesion obtenerSesion(
            UsuarioService usuarioService) {
        return new ObtenerSesion(usuarioService);
    }

    @Bean
    public RegistroService registroService(
            UsuarioRepository usuarioRepository,
            NegocioRepository negocioRepository,
            ContraseniaPort contraseniaPort,
            SuscripcionService suscripcionService,
            TransaccionPort transaccionPort) {
        return new RegistroService(
                usuarioRepository, negocioRepository, contraseniaPort, suscripcionService, transaccionPort);
    }

    @Bean
    public UsuarioService usuarioService(UsuarioRepository usuarioRepository, ContraseniaPort contraseniaPort) {
        return new UsuarioService(usuarioRepository, contraseniaPort);
    }
}

