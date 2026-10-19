package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.negocio.ActualizarNegocioMapper;
import com.ensambla.backend.domain.mapper.negocio.ObtenerConfiguracionNegocioMapper;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.negocio.ActualizarNegocioUseCase;
import com.ensambla.backend.domain.useCase.negocio.ObtenerConfiguracionNegocioUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class NegocioUseCaseConfig {

    @Bean
    public ObtenerConfiguracionNegocioMapper obtenerConfiguracionNegocioMapper() {
        return new ObtenerConfiguracionNegocioMapper();
    }

    @Bean
    public ActualizarNegocioMapper actualizarNegocioMapper(ObtenerConfiguracionNegocioMapper mapper) {
        return new ActualizarNegocioMapper(mapper);
    }

    @Bean
    public ObtenerConfiguracionNegocioUseCase obtenerConfiguracionNegocioUseCase(
            NegocioService negocioService, ObtenerConfiguracionNegocioMapper mapper) {
        return new ObtenerConfiguracionNegocioUseCase(negocioService, mapper);
    }

    @Bean
    public ActualizarNegocioUseCase actualizarNegocioUseCase(
            NegocioService negocioService, ActualizarNegocioMapper mapper, Clock clock) {
        return new ActualizarNegocioUseCase(negocioService, mapper, clock);
    }
}
