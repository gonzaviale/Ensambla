package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.proyecto.ProyectoItemMapper;
import com.ensambla.backend.domain.mapper.sami.ConversarConSamiMapper;
import com.ensambla.backend.domain.mapper.sami.ConversarEnTallerMapper;
import com.ensambla.backend.domain.mapper.sami.CrearProyectoDesdeSamiMapper;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.service.sami.ConversacionSamiService;
import com.ensambla.backend.domain.service.sami.RespaldoIaSamiService;
import com.ensambla.backend.domain.service.sami.TallerSamiService;
import com.ensambla.backend.domain.useCase.sami.ConversarConSamiUseCase;
import com.ensambla.backend.domain.useCase.sami.ConversarEnTallerUseCase;
import com.ensambla.backend.domain.useCase.sami.CrearProyectoDesdeSamiUseCase;
import com.ensambla.backend.infrastructure.ia.SamiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SamiUseCaseConfig {

    @Bean
    public ConversarConSamiMapper conversarConSamiMapper() {
        return new ConversarConSamiMapper();
    }

    @Bean
    public CrearProyectoDesdeSamiMapper crearProyectoDesdeSamiMapper(ProyectoItemMapper proyectoItemMapper) {
        return new CrearProyectoDesdeSamiMapper(proyectoItemMapper);
    }

    @Bean
    public CrearProyectoDesdeSamiUseCase crearProyectoDesdeSamiUseCase(
            NegocioService negocioService,
            PlantillaService plantillaService,
            ProyectoService proyectoService,
            CrearProyectoDesdeSamiMapper crearProyectoDesdeSamiMapper,
            Clock clock) {
        return new CrearProyectoDesdeSamiUseCase(
                negocioService, plantillaService, proyectoService, crearProyectoDesdeSamiMapper, clock);
    }

    @Bean
    public ConversarEnTallerMapper conversarEnTallerMapper() {
        return new ConversarEnTallerMapper();
    }

    @Bean
    public ConversarEnTallerUseCase conversarEnTallerUseCase(NegocioService negocioService,
                                                             TallerSamiService tallerSamiService,
                                                             ConversarEnTallerMapper conversarEnTallerMapper,
                                                             Clock clock,
                                                             SamiProperties samiProperties) {
        return new ConversarEnTallerUseCase(
                negocioService,
                tallerSamiService,
                conversarEnTallerMapper,
                clock,
                samiProperties.limiteDiarioTaller());
    }

    @Bean
    public ConversarConSamiUseCase conversarConSamiUseCase(NegocioService negocioService,
                                                           PlantillaService plantillaService,
                                                           ConversacionSamiService conversacionSamiService,
                                                           RespaldoIaSamiService respaldoIaSamiService,
                                                           ConversarConSamiMapper conversarConSamiMapper,
                                                           Clock clock,
                                                           SamiProperties samiProperties) {
        return new ConversarConSamiUseCase(
                negocioService,
                plantillaService,
                conversacionSamiService,
                respaldoIaSamiService,
                conversarConSamiMapper,
                clock,
                samiProperties.umbralConfianza(),
                samiProperties.maxPreguntas(),
                samiProperties.limiteDiarioIa());
    }
}
