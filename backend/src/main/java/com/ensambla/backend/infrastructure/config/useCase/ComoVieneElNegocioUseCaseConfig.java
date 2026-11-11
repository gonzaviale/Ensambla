package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio.ObtenerComoVieneElNegocio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ComoVieneElNegocioUseCaseConfig {

    @Bean
    public ObtenerComoVieneElNegocio obtenerComoVieneElNegocio(
            NegocioRepository negocioRepository,
            SistemaRepository sistemaRepository,
            DatosDeProyectoService datosDeProyectoService,
            Clock clock) {
        return new ObtenerComoVieneElNegocio(negocioRepository, sistemaRepository, datosDeProyectoService, clock);
    }
}
