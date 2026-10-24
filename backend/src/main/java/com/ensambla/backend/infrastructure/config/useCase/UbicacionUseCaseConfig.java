package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.ubicacion.BuscarDireccionesMapper;
import com.ensambla.backend.domain.mapper.ubicacion.ListarRubrosMapper;
import com.ensambla.backend.domain.mapper.ubicacion.ResolverLocalidadMapper;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.domain.service.ubicacion.RubroService;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.useCase.ubicacion.BuscarDireccionesUseCase;
import com.ensambla.backend.domain.useCase.ubicacion.ListarRubrosUseCase;
import com.ensambla.backend.domain.useCase.ubicacion.ResolverLocalidadUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UbicacionUseCaseConfig {

    @Bean
    public BuscarDireccionesMapper buscarDireccionesMapper() {
        return new BuscarDireccionesMapper();
    }

    @Bean
    public ListarRubrosMapper listarRubrosMapper() {
        return new ListarRubrosMapper();
    }

    @Bean
    public BuscarDireccionesUseCase buscarDireccionesUseCase(
            UbicacionService ubicacionService,
            BuscarDireccionesMapper buscarDireccionesMapper,
            Clock clock) {
        return new BuscarDireccionesUseCase(ubicacionService, buscarDireccionesMapper, clock);
    }

    @Bean
    public ResolverLocalidadMapper resolverLocalidadMapper() {
        return new ResolverLocalidadMapper();
    }

    @Bean
    public ResolverLocalidadUseCase resolverLocalidadUseCase(
            UbicacionService ubicacionService,
            ResolverLocalidadMapper resolverLocalidadMapper,
            Clock clock) {
        return new ResolverLocalidadUseCase(ubicacionService, resolverLocalidadMapper, clock);
    }

    @Bean
    public ListarRubrosUseCase listarRubrosUseCase(RubroService rubroService, ListarRubrosMapper listarRubrosMapper) {
        return new ListarRubrosUseCase(rubroService, listarRubrosMapper);
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
