package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.bloque.BloqueItemMapper;
import com.ensambla.backend.domain.mapper.bloque.CrearBloqueMapper;
import com.ensambla.backend.domain.mapper.bloque.ListarBloquesMapper;
import com.ensambla.backend.domain.mapper.bloque.RenombrarBloqueMapper;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.bloque.CrearBloqueUseCase;
import com.ensambla.backend.domain.useCase.bloque.EliminarBloqueUseCase;
import com.ensambla.backend.domain.useCase.bloque.ListarBloquesUseCase;
import com.ensambla.backend.domain.useCase.bloque.RenombrarBloqueUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BloqueUseCaseConfig {

    @Bean
    public BloqueItemMapper bloqueItemMapper() {
        return new BloqueItemMapper();
    }

    @Bean
    public ListarBloquesMapper listarBloquesMapper(BloqueItemMapper bloqueItemMapper) {
        return new ListarBloquesMapper(bloqueItemMapper);
    }

    @Bean
    public ListarBloquesUseCase listarBloquesUseCase(
            NegocioService negocioService,
            ProyectoService proyectoService,
            ListarBloquesMapper listarBloquesMapper) {
        return new ListarBloquesUseCase(
                negocioService,
                proyectoService,
                listarBloquesMapper);
    }

    @Bean
    public CrearBloqueMapper crearBloqueMapper(BloqueItemMapper bloqueItemMapper) {
        return new CrearBloqueMapper(bloqueItemMapper);
    }

    @Bean
    public CrearBloqueUseCase crearBloqueUseCase(
            NegocioService negocioService,
            ProyectoService proyectoService,
            BloqueService bloqueService,
            CrearBloqueMapper crearBloqueMapper,
            Clock clock) {
        return new CrearBloqueUseCase(
                negocioService,
                proyectoService,
                bloqueService,
                crearBloqueMapper,
                clock);
    }

    @Bean
    public RenombrarBloqueMapper renombrarBloqueMapper(BloqueItemMapper bloqueItemMapper) {
        return new RenombrarBloqueMapper(bloqueItemMapper);
    }

    @Bean
    public RenombrarBloqueUseCase renombrarBloqueUseCase(
            NegocioService negocioService,
            BloqueService bloqueService,
            ProyectoService proyectoService,
            RenombrarBloqueMapper renombrarBloqueMapper,
            Clock clock) {
        return new RenombrarBloqueUseCase(
                negocioService,
                bloqueService,
                proyectoService,
                renombrarBloqueMapper,
                clock);
    }

    @Bean
    public EliminarBloqueUseCase eliminarBloqueUseCase(
            NegocioService negocioService,
            BloqueService bloqueService) {
        return new EliminarBloqueUseCase(
                negocioService,
                bloqueService);
    }
}
