package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
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
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository) {
        return new ListarProyectos(
                negocioRepository,
                proyectoRepository);
    }

    @Bean
    public ObtenerProyecto obtenerProyecto(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository) {
        return new ObtenerProyecto(
                negocioRepository,
                proyectoRepository);
    }

    @Bean
    public CrearProyecto crearProyecto(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository,
            ControlPlanService controlPlanService,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new CrearProyecto(
                negocioRepository,
                proyectoRepository,
                controlPlanService,
                transaccionPort,
                clock);
    }

    @Bean
    public RenombrarProyecto renombrarProyecto(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new RenombrarProyecto(
                negocioRepository,
                proyectoRepository,
                transaccionPort,
                clock);
    }

    @Bean
    public CrearProyectoDesdePlantilla crearProyectoDesdePlantilla(
            NegocioRepository negocioRepository,
            PlantillaRepository plantillaRepository,
            ProyectoRepository proyectoRepository,
            ControlPlanService controlPlanService,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new CrearProyectoDesdePlantilla(
                negocioRepository,
                plantillaRepository,
                proyectoRepository,
                controlPlanService,
                transaccionPort,
                clock);
    }

    @Bean
    public EliminarProyecto eliminarProyecto(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository,
            TransaccionPort transaccionPort) {
        return new EliminarProyecto(negocioRepository, proyectoRepository, transaccionPort);
    }

}

