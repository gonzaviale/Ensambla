package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.service.zona.ProductosPropiosService;
import com.ensambla.backend.domain.useCase.zona.analizarZona.AnalizarZona;
import com.ensambla.backend.domain.useCase.zona.obtenerZona.ObtenerZona;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ZonaUseCaseConfig {

    @Bean
    public ProductosPropiosService productosPropiosService(
            ProyectoRepository proyectoRepository,
            DatosDeProyectoService datosDeProyectoService) {
        return new ProductosPropiosService(proyectoRepository, datosDeProyectoService);
    }

    @Bean
    public ObtenerZona obtenerZona(
            NegocioRepository negocioRepository,
            ZonaRepository zonaRepository,
            AgregadoZonaRepository agregadoZonaRepository,
            ControlPlanService controlPlanService,
            ProductosPropiosService productosPropiosService,
            Clock clock) {
        return new ObtenerZona(negocioRepository, zonaRepository, agregadoZonaRepository, controlPlanService,
                productosPropiosService, clock);
    }

    @Bean
    public AnalizarZona analizarZona(
            ZonaRepository zonaRepository,
            GeocodificadorPort geocodificadorPort,
            AgregadoZonaRepository agregadoZonaRepository,
            Clock clock,
            NegocioRepository negocioRepository,
            ControlPlanService controlPlanService) {
        return new AnalizarZona(zonaRepository, geocodificadorPort, agregadoZonaRepository, clock,
                negocioRepository, controlPlanService);
    }
}
