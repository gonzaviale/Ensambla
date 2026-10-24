package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.plan.ConsultarPlanesMapper;
import com.ensambla.backend.domain.mapper.plan.IniciarPruebaMapper;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.AccesoContenidoRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.service.plan.ConsultaPlanesService;
import com.ensambla.backend.domain.service.plan.ConsumoIaInmediatoService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.plan.PruebaPremiumService;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.domain.useCase.plan.ConsultarPlanesUseCase;
import com.ensambla.backend.domain.useCase.plan.DesbloquearContenidosUseCase;
import com.ensambla.backend.domain.useCase.plan.IniciarPruebaUseCase;
import com.ensambla.backend.domain.useCase.plan.VencerPruebasUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PlanUseCaseConfig {

    @Bean
    public DesbloquearContenidosUseCase desbloquearContenidosUseCase(
            NegocioService negocioService, AccesoContenidoService accesoContenidoService, Clock clock) {
        return new DesbloquearContenidosUseCase(negocioService, accesoContenidoService, clock);
    }

    @Bean
    public ConsultarPlanesMapper consultarPlanesMapper() {
        return new ConsultarPlanesMapper();
    }

    @Bean
    public ConsultarPlanesUseCase consultarPlanesUseCase(NegocioService negocioService,
                                                       ConsultaPlanesService consultaPlanesService,
                                                       ConsultarPlanesMapper consultarPlanesMapper,
                                                       Clock clock) {
        return new ConsultarPlanesUseCase(negocioService, consultaPlanesService, consultarPlanesMapper, clock);
    }

    @Bean
    public IniciarPruebaMapper iniciarPruebaMapper() {
        return new IniciarPruebaMapper();
    }

    @Bean
    public IniciarPruebaUseCase iniciarPruebaUseCase(NegocioService negocioService,
                                                   PruebaPremiumService pruebaPremiumService,
                                                   IniciarPruebaMapper iniciarPruebaMapper,
                                                   Clock clock) {
        return new IniciarPruebaUseCase(negocioService, pruebaPremiumService, iniciarPruebaMapper, clock);
    }

    @Bean
    public VencerPruebasUseCase vencerPruebasUseCase(PruebaPremiumService pruebaPremiumService, Clock clock) {
        return new VencerPruebasUseCase(pruebaPremiumService, clock);
    }

    @Bean
    public AccesoContenidoService accesoContenidoService(
            ControlPlanService controlPlanService,
            UsoPlanRepository usoPlanRepository,
            AccesoContenidoRepository accesoContenidoRepository,
            TransaccionPort transaccionPort) {
        return new AccesoContenidoService(
                controlPlanService, usoPlanRepository, accesoContenidoRepository, transaccionPort);
    }

    @Bean
    public ConsultaPlanesService consultaPlanesService(
            PlanRepository planRepository,
            SuscripcionService suscripcionService,
            PruebaPremiumRepository pruebaPremiumRepository,
            UsoPlanRepository usoPlanRepository,
            RenovacionPremiumRepository renovacionPremiumRepository,
            TransaccionPort transaccionPort) {
        return new ConsultaPlanesService(
                planRepository, suscripcionService, pruebaPremiumRepository, usoPlanRepository, renovacionPremiumRepository, transaccionPort);
    }

    @Bean
    public ConsumoIaInmediatoService consumoIaInmediatoService(
            ControlPlanService controlPlanService,
            UsoPlanRepository usoPlanRepository,
            PoliticaProcesamiento politicaProcesamiento,
            TransaccionPort transaccionPort) {
        return new ConsumoIaInmediatoService(
                controlPlanService, usoPlanRepository, politicaProcesamiento, transaccionPort);
    }

    @Bean
    public ControlPlanService controlPlanService(
            PlanRepository planRepository,
            SuscripcionRepository suscripcionRepository,
            UsoPlanRepository usoPlanRepository,
            PruebaPremiumRepository pruebaPremiumRepository,
            TransaccionPort transaccionPort) {
        return new ControlPlanService(
                planRepository, suscripcionRepository, usoPlanRepository, pruebaPremiumRepository, transaccionPort);
    }

    @Bean
    public PruebaPremiumService pruebaPremiumService(
            PruebaPremiumRepository pruebaPremiumRepository,
            SuscripcionRepository suscripcionRepository,
            PlanRepository planRepository,
            UsoPlanRepository usoPlanRepository,
            RenovacionPremiumRepository renovacionPremiumRepository,
            TransaccionPort transaccionPort) {
        return new PruebaPremiumService(
                pruebaPremiumRepository, suscripcionRepository, planRepository, usoPlanRepository, renovacionPremiumRepository, transaccionPort);
    }

    @Bean
    public SuscripcionService suscripcionService(
            PlanRepository planRepository,
            SuscripcionRepository suscripcionRepository,
            TransaccionPort transaccionPort) {
        return new SuscripcionService(planRepository, suscripcionRepository, transaccionPort);
    }
}
