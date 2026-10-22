package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.zona.ZonaItemMapper;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.service.zona.AgregadoZonaService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import com.ensambla.backend.domain.useCase.zona.AnalizarZonaUseCase;
import com.ensambla.backend.domain.useCase.zona.ObtenerZonaUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ZonaUseCaseConfig {

    @Bean
    public ZonaItemMapper zonaItemMapper() {
        return new ZonaItemMapper();
    }

    @Bean
    public ObtenerZonaUseCase obtenerZonaUseCase(
            NegocioService negocioService,
            ZonaService zonaService,
            AgregadoZonaService agregadoZonaService,
            ZonaItemMapper zonaItemMapper) {
        return new ObtenerZonaUseCase(negocioService, zonaService, agregadoZonaService, zonaItemMapper);
    }

    @Bean
    public AnalizarZonaUseCase analizarZonaUseCase(
            ZonaService zonaService,
            UbicacionService ubicacionService,
            AgregadoZonaService agregadoZonaService,
            ZonaItemMapper zonaItemMapper,
            Clock clock) {
        return new AnalizarZonaUseCase(zonaService, ubicacionService, agregadoZonaService, zonaItemMapper, clock);
    }
}
