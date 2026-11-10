package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
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
            ProyectoRepository proyectoRepository,
            DatosDeProyectoService datosDeProyectoService,
            Clock clock) {
        return new ObtenerComoVieneElNegocio(negocioRepository, proyectoRepository, datosDeProyectoService, clock);
    }
}
