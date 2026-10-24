package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.port.SamiIaPort;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSami;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTaller;
import com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami.CrearProyectoDesdeSami;
import com.ensambla.backend.infrastructure.ia.SamiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SamiUseCaseConfig {

    @Bean
    public CrearProyectoDesdeSami crearProyectoDesdeSami(
            NegocioService negocioService,
            PlantillaService plantillaService,
            ProyectoService proyectoService,
            Clock clock) {
        return new CrearProyectoDesdeSami(
                negocioService, plantillaService, proyectoService, clock);
    }

    @Bean
    public ConversarEnTaller conversarEnTaller(NegocioService negocioService,
                                               TallerIaPort tallerIaPort,
                                               ConsumoSamiRepository consumoSamiRepository,
                                               CatalogoComponentes catalogoComponentes,
                                               KitsRubro kitsRubro,
                                               Clock clock,
                                               SamiProperties samiProperties) {
        return new ConversarEnTaller(negocioService, tallerIaPort, consumoSamiRepository, catalogoComponentes,
                kitsRubro, clock, samiProperties.limiteDiarioTaller());
    }

    @Bean
    public ConversarConSami conversarConSami(NegocioService negocioService,
                                             PlantillaRepository plantillaRepository,
                                             DiccionarioSami diccionario,
                                             SamiIaPort samiIaPort,
                                             ConsumoSamiRepository consumoSamiRepository,
                                             Clock clock,
                                             SamiProperties samiProperties) {
        return new ConversarConSami(negocioService, plantillaRepository, diccionario, samiIaPort,
                consumoSamiRepository, clock, samiProperties.umbralConfianza(), samiProperties.maxPreguntas(),
                samiProperties.limiteDiarioIa());
    }
}
