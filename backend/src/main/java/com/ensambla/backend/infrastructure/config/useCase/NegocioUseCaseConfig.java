package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.negocio.ActualizarNegocioMapper;
import com.ensambla.backend.domain.mapper.negocio.ObtenerConfiguracionNegocioMapper;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.service.zona.ZonaService;
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
            NegocioService negocioService, ZonaService zonaService, ObtenerConfiguracionNegocioMapper mapper) {
        return new ObtenerConfiguracionNegocioUseCase(negocioService, zonaService, mapper);
    }

    @Bean
    public ActualizarNegocioUseCase actualizarNegocioUseCase(
            NegocioService negocioService, ZonaService zonaService, ActualizarNegocioMapper mapper, Clock clock) {
        return new ActualizarNegocioUseCase(negocioService, zonaService, mapper, clock);
    }

    @Bean
    public NegocioService negocioService(
            NegocioRepository negocioRepository,
            ZonaService zonaService,
            UbicacionService ubicacionService,
            TransaccionPort transaccionPort) {
        return new NegocioService(negocioRepository, zonaService, ubicacionService, transaccionPort);
    }
}
