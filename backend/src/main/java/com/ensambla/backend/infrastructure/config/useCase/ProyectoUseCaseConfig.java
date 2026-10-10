package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.proyecto.CrearProyectoDesdePlantillaMapper;
import com.ensambla.backend.domain.mapper.proyecto.CrearProyectoMapper;
import com.ensambla.backend.domain.mapper.proyecto.ListarProyectosMapper;
import com.ensambla.backend.domain.mapper.proyecto.ObtenerProyectoMapper;
import com.ensambla.backend.domain.mapper.proyecto.ProyectoItemMapper;
import com.ensambla.backend.domain.mapper.proyecto.RenombrarProyectoMapper;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.proyecto.CrearProyectoDesdePlantillaUseCase;
import com.ensambla.backend.domain.useCase.proyecto.CrearProyectoUseCase;
import com.ensambla.backend.domain.useCase.proyecto.EliminarProyectoUseCase;
import com.ensambla.backend.domain.useCase.proyecto.ListarProyectosUseCase;
import com.ensambla.backend.domain.useCase.proyecto.ObtenerProyectoUseCase;
import com.ensambla.backend.domain.useCase.proyecto.RenombrarProyectoUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ProyectoUseCaseConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ProyectoItemMapper proyectoItemMapper() {
        return new ProyectoItemMapper();
    }

    @Bean
    public ListarProyectosMapper listarProyectosMapper(ProyectoItemMapper proyectoItemMapper) {
        return new ListarProyectosMapper(proyectoItemMapper);
    }

    @Bean
    public ListarProyectosUseCase listarProyectosUseCase(
            NegocioService negocioService,
            ProyectoService proyectoService,
            ListarProyectosMapper listarProyectosMapper) {
        return new ListarProyectosUseCase(
                negocioService,
                proyectoService,
                listarProyectosMapper);
    }

    @Bean
    public ObtenerProyectoMapper obtenerProyectoMapper(ProyectoItemMapper proyectoItemMapper) {
        return new ObtenerProyectoMapper(proyectoItemMapper);
    }

    @Bean
    public ObtenerProyectoUseCase obtenerProyectoUseCase(
            NegocioService negocioService,
            ProyectoService proyectoService,
            ObtenerProyectoMapper obtenerProyectoMapper) {
        return new ObtenerProyectoUseCase(
                negocioService,
                proyectoService,
                obtenerProyectoMapper);
    }

    @Bean
    public CrearProyectoMapper crearProyectoMapper(ProyectoItemMapper proyectoItemMapper) {
        return new CrearProyectoMapper(proyectoItemMapper);
    }

    @Bean
    public CrearProyectoUseCase crearProyectoUseCase(
            NegocioService negocioService,
            ProyectoService proyectoService,
            CrearProyectoMapper crearProyectoMapper,
            Clock clock) {
        return new CrearProyectoUseCase(
                negocioService,
                proyectoService,
                crearProyectoMapper,
                clock);
    }

    @Bean
    public RenombrarProyectoMapper renombrarProyectoMapper(ProyectoItemMapper proyectoItemMapper) {
        return new RenombrarProyectoMapper(proyectoItemMapper);
    }

    @Bean
    public RenombrarProyectoUseCase renombrarProyectoUseCase(
            NegocioService negocioService,
            ProyectoService proyectoService,
            RenombrarProyectoMapper renombrarProyectoMapper,
            Clock clock) {
        return new RenombrarProyectoUseCase(
                negocioService,
                proyectoService,
                renombrarProyectoMapper,
                clock);
    }

    @Bean
    public CrearProyectoDesdePlantillaMapper crearProyectoDesdePlantillaMapper(ProyectoItemMapper proyectoItemMapper) {
        return new CrearProyectoDesdePlantillaMapper(proyectoItemMapper);
    }

    @Bean
    public CrearProyectoDesdePlantillaUseCase crearProyectoDesdePlantillaUseCase(
            NegocioService negocioService,
            ProyectoService proyectoService,
            PlantillaService plantillaService,
            CrearProyectoDesdePlantillaMapper crearProyectoDesdePlantillaMapper,
            Clock clock) {
        return new CrearProyectoDesdePlantillaUseCase(
                negocioService,
                proyectoService,
                plantillaService,
                crearProyectoDesdePlantillaMapper,
                clock);
    }

    @Bean
    public EliminarProyectoUseCase eliminarProyectoUseCase(
            NegocioService negocioService, ProyectoService proyectoService) {
        return new EliminarProyectoUseCase(negocioService, proyectoService);
    }
}

