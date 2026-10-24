package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.proyecto.crearProyecto.CrearProyecto;
import com.ensambla.backend.domain.useCase.proyecto.crearProyectoDesdePlantilla.CrearProyectoDesdePlantilla;
import com.ensambla.backend.domain.useCase.proyecto.eliminarProyecto.EliminarProyecto;
import com.ensambla.backend.domain.useCase.proyecto.listarProyectos.ListarProyectos;
import com.ensambla.backend.domain.useCase.proyecto.obtenerProyecto.ObtenerProyecto;
import com.ensambla.backend.domain.useCase.proyecto.renombrarProyecto.RenombrarProyecto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ProyectoUseCaseConfig {

    @Bean
    public ListarProyectos listarProyectos(
            NegocioService negocioService,
            ProyectoService proyectoService) {
        return new ListarProyectos(
                negocioService,
                proyectoService);
    }

    @Bean
    public ObtenerProyecto obtenerProyecto(
            NegocioService negocioService,
            ProyectoService proyectoService) {
        return new ObtenerProyecto(
                negocioService,
                proyectoService);
    }

    @Bean
    public CrearProyecto crearProyecto(
            NegocioService negocioService,
            ProyectoService proyectoService,
            Clock clock) {
        return new CrearProyecto(
                negocioService,
                proyectoService,
                clock);
    }

    @Bean
    public RenombrarProyecto renombrarProyecto(
            NegocioService negocioService,
            ProyectoService proyectoService,
            Clock clock) {
        return new RenombrarProyecto(
                negocioService,
                proyectoService,
                clock);
    }

    @Bean
    public CrearProyectoDesdePlantilla crearProyectoDesdePlantilla(
            NegocioService negocioService,
            ProyectoService proyectoService,
            PlantillaService plantillaService,
            Clock clock) {
        return new CrearProyectoDesdePlantilla(
                negocioService,
                proyectoService,
                plantillaService,
                clock);
    }

    @Bean
    public EliminarProyecto eliminarProyecto(
            NegocioService negocioService, ProyectoService proyectoService) {
        return new EliminarProyecto(negocioService, proyectoService);
    }

    @Bean
    public ProyectoService proyectoService(
            ProyectoRepository proyectoRepository,
            ControlPlanService controlPlanService,
            TransaccionPort transaccionPort) {
        return new ProyectoService(proyectoRepository, controlPlanService, transaccionPort);
    }
}

