package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.port.SamiIaPort;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.service.sami.ConversacionSamiService;
import com.ensambla.backend.domain.service.sami.HerramientasTallerService;
import com.ensambla.backend.domain.service.sami.InterpretacionSamiService;
import com.ensambla.backend.domain.service.sami.RespaldoIaSamiService;
import com.ensambla.backend.domain.service.sami.RubroTallerService;
import com.ensambla.backend.domain.service.sami.TallerSamiService;
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
                                               TallerSamiService tallerSamiService,
                                               Clock clock,
                                               SamiProperties samiProperties) {
        return new ConversarEnTaller(
                negocioService,
                tallerSamiService,
                clock,
                samiProperties.limiteDiarioTaller());
    }

    @Bean
    public ConversarConSami conversarConSami(NegocioService negocioService,
                                             PlantillaService plantillaService,
                                             ConversacionSamiService conversacionSamiService,
                                             RespaldoIaSamiService respaldoIaSamiService,
                                             Clock clock,
                                             SamiProperties samiProperties) {
        return new ConversarConSami(
                negocioService,
                plantillaService,
                conversacionSamiService,
                respaldoIaSamiService,
                clock,
                samiProperties.umbralConfianza(),
                samiProperties.maxPreguntas(),
                samiProperties.limiteDiarioIa());
    }

    @Bean
    public ConversacionSamiService conversacionSamiService(
            InterpretacionSamiService interpretacionSamiService,
            DiccionarioSami diccionario) {
        return new ConversacionSamiService(interpretacionSamiService, diccionario);
    }

    @Bean
    public HerramientasTallerService herramientasTallerService(
            CatalogoComponentes catalogoComponentes,
            KitsRubro kitsRubro) {
        return new HerramientasTallerService(catalogoComponentes, kitsRubro);
    }

    @Bean
    public InterpretacionSamiService interpretacionSamiService(DiccionarioSami diccionario) {
        return new InterpretacionSamiService(diccionario);
    }

    @Bean
    public RespaldoIaSamiService respaldoIaSamiService(
            SamiIaPort samiIaPort,
            ConsumoSamiRepository consumoSamiRepository,
            DiccionarioSami diccionario) {
        return new RespaldoIaSamiService(samiIaPort, consumoSamiRepository, diccionario);
    }

    @Bean
    public RubroTallerService rubroTallerService(KitsRubro kitsRubro) {
        return new RubroTallerService(kitsRubro);
    }

    @Bean
    public TallerSamiService tallerSamiService(
            TallerIaPort tallerIaPort,
            ConsumoSamiRepository consumoSamiRepository,
            HerramientasTallerService herramientasTallerService,
            RubroTallerService rubroTallerService,
            CatalogoComponentes catalogoComponentes) {
        return new TallerSamiService(
                tallerIaPort, consumoSamiRepository, herramientasTallerService, rubroTallerService, catalogoComponentes);
    }
}
