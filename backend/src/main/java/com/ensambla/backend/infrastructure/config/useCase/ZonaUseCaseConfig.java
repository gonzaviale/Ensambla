package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.service.zona.AccesoZonaService;
import com.ensambla.backend.domain.service.zona.AgregadoZonaService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import com.ensambla.backend.domain.useCase.zona.analizarZona.AnalizarZona;
import com.ensambla.backend.domain.useCase.zona.obtenerZona.ObtenerZona;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ZonaUseCaseConfig {

    @Bean
    public ObtenerZona obtenerZona(
            NegocioService negocioService,
            ZonaService zonaService,
            AgregadoZonaService agregadoZonaService,
            AccesoZonaService accesoZonaService, Clock clock) {
        return new ObtenerZona(negocioService, zonaService, agregadoZonaService,
                accesoZonaService, clock);
    }

    @Bean
    public AnalizarZona analizarZona(
            ZonaService zonaService,
            ZonaRepository zonaRepository,
            UbicacionService ubicacionService,
            AgregadoZonaService agregadoZonaService,
            Clock clock, NegocioService negocioService, AccesoZonaService accesoZonaService) {
        return new AnalizarZona(zonaService, zonaRepository, ubicacionService, agregadoZonaService, clock,
                negocioService, accesoZonaService);
    }

    @Bean
    public AccesoZonaService accesoZonaService(AccesoContenidoService accesoContenidoService) {
        return new AccesoZonaService(accesoContenidoService);
    }

    @Bean
    public AgregadoZonaService agregadoZonaService(AgregadoZonaRepository agregadoZonaRepository) {
        return new AgregadoZonaService(agregadoZonaRepository);
    }

    @Bean
    public ZonaService zonaService(ZonaRepository zonaRepository) {
        return new ZonaService(zonaRepository);
    }
}
