package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.port.SamiIaPort;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
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
            NegocioRepository negocioRepository,
            PlantillaRepository plantillaRepository,
            ProyectoRepository proyectoRepository,
            ControlPlanService controlPlanService,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new CrearProyectoDesdeSami(
                negocioRepository, plantillaRepository, proyectoRepository, controlPlanService, transaccionPort,
                clock);
    }

    @Bean
    public ConversarEnTaller conversarEnTaller(NegocioRepository negocioRepository,
                                               TallerIaPort tallerIaPort,
                                               ConsumoSamiRepository consumoSamiRepository,
                                               CatalogoComponentes catalogoComponentes,
                                               KitsRubro kitsRubro,
                                               Clock clock,
                                               SamiProperties samiProperties) {
        return new ConversarEnTaller(negocioRepository, tallerIaPort, consumoSamiRepository, catalogoComponentes,
                kitsRubro, clock, samiProperties.limiteDiarioTaller());
    }

    @Bean
    public ConversarConSami conversarConSami(NegocioRepository negocioRepository,
                                             PlantillaRepository plantillaRepository,
                                             DiccionarioSami diccionario,
                                             SamiIaPort samiIaPort,
                                             ConsumoSamiRepository consumoSamiRepository,
                                             Clock clock,
                                             SamiProperties samiProperties) {
        return new ConversarConSami(negocioRepository, plantillaRepository, diccionario, samiIaPort,
                consumoSamiRepository, clock, samiProperties.umbralConfianza(), samiProperties.maxPreguntas(),
                samiProperties.limiteDiarioIa());
    }
}
