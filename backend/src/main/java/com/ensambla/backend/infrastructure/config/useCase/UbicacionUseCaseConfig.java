package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.domain.service.ubicacion.RubroService;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.useCase.ubicacion.buscarDirecciones.BuscarDirecciones;
import com.ensambla.backend.domain.useCase.ubicacion.listarRubros.ListarRubros;
import com.ensambla.backend.domain.useCase.ubicacion.resolverLocalidad.ResolverLocalidad;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UbicacionUseCaseConfig {

    @Bean
    public BuscarDirecciones buscarDirecciones(
            UbicacionService ubicacionService,
            Clock clock) {
        return new BuscarDirecciones(ubicacionService, clock);
    }

    @Bean
    public ResolverLocalidad resolverLocalidad(
            UbicacionService ubicacionService,
            Clock clock) {
        return new ResolverLocalidad(ubicacionService, clock);
    }

    @Bean
    public ListarRubros listarRubros(RubroService rubroService) {
        return new ListarRubros(rubroService);
    }

    @Bean
    public RubroService rubroService() {
        return new RubroService();
    }

    @Bean
    public UbicacionService ubicacionService(GeocodificadorPort geocodificadorPort) {
        return new UbicacionService(geocodificadorPort);
    }
}
