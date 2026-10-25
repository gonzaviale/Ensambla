package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.domain.useCase.ubicacion.buscarDirecciones.BuscarDirecciones;
import com.ensambla.backend.domain.useCase.ubicacion.listarRubros.ListarRubros;
import com.ensambla.backend.domain.useCase.ubicacion.resolverLocalidad.ResolverLocalidad;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UbicacionUseCaseConfig {

    @Bean
    public BuscarDirecciones buscarDirecciones(GeocodificadorPort geocodificadorPort, Clock clock) {
        return new BuscarDirecciones(geocodificadorPort, clock);
    }

    @Bean
    public ResolverLocalidad resolverLocalidad(GeocodificadorPort geocodificadorPort, Clock clock) {
        return new ResolverLocalidad(geocodificadorPort, clock);
    }

    @Bean
    public ListarRubros listarRubros() {
        return new ListarRubros();
    }
}
