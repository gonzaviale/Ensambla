package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import com.ensambla.backend.domain.useCase.negocio.actualizarNegocio.ActualizarNegocio;
import com.ensambla.backend.domain.useCase.negocio.obtenerConfiguracionNegocio.ObtenerConfiguracionNegocio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class NegocioUseCaseConfig {

    @Bean
    public ObtenerConfiguracionNegocio obtenerConfiguracionNegocio(
            NegocioService negocioService, ZonaService zonaService) {
        return new ObtenerConfiguracionNegocio(negocioService, zonaService);
    }

    @Bean
    public ActualizarNegocio actualizarNegocio(
            NegocioService negocioService, ZonaService zonaService, Clock clock) {
        return new ActualizarNegocio(negocioService, zonaService, clock);
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
