package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.auth.IniciarSesionMapper;
import com.ensambla.backend.domain.mapper.auth.ObtenerSesionMapper;
import com.ensambla.backend.domain.mapper.auth.RegistrarUsuarioMapper;
import com.ensambla.backend.domain.service.auth.RegistroService;
import com.ensambla.backend.domain.service.usuario.UsuarioService;
import com.ensambla.backend.domain.useCase.auth.IniciarSesionUseCase;
import com.ensambla.backend.domain.useCase.auth.ObtenerSesionUseCase;
import com.ensambla.backend.domain.useCase.auth.RegistrarUsuarioUseCase;
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
    public RegistrarUsuarioMapper registrarUsuarioMapper() {
        return new RegistrarUsuarioMapper();
    }

    @Bean
    public IniciarSesionMapper iniciarSesionMapper() {
        return new IniciarSesionMapper();
    }

    @Bean
    public ObtenerSesionMapper obtenerSesionMapper() {
        return new ObtenerSesionMapper();
    }

    @Bean
    public RegistrarUsuarioUseCase registrarUsuarioUseCase(
            RegistroService registroService, RegistrarUsuarioMapper registrarUsuarioMapper, Clock clock) {
        return new RegistrarUsuarioUseCase(registroService, registrarUsuarioMapper, clock);
    }

    @Bean
    public IniciarSesionUseCase iniciarSesionUseCase(
            UsuarioService usuarioService, IniciarSesionMapper iniciarSesionMapper) {
        return new IniciarSesionUseCase(usuarioService, iniciarSesionMapper);
    }

    @Bean
    public ObtenerSesionUseCase obtenerSesionUseCase(
            UsuarioService usuarioService, ObtenerSesionMapper obtenerSesionMapper) {
        return new ObtenerSesionUseCase(usuarioService, obtenerSesionMapper);
    }
}

