package com.ensambla.backend.infrastructure.config.useCase;

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
import com.ensambla.backend.domain.useCase.plan.consultarPlanes.ConsultarPlanes;
import com.ensambla.backend.domain.useCase.plan.desbloquearContenidos.DesbloquearContenidos;
import com.ensambla.backend.domain.useCase.plan.iniciarPrueba.IniciarPrueba;
import com.ensambla.backend.domain.useCase.plan.vencerPruebas.VencerPruebas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PlanUseCaseConfig {

    @Bean
    public DesbloquearContenidos desbloquearContenidos(
            NegocioService negocioService, AccesoContenidoService accesoContenidoService, Clock clock) {
        return new DesbloquearContenidos(negocioService, accesoContenidoService, clock);
    }

    @Bean
    public ConsultarPlanes consultarPlanes(NegocioService negocioService,
                                           ConsultaPlanesService consultaPlanesService,
                                           Clock clock) {
        return new ConsultarPlanes(negocioService, consultaPlanesService, clock);
    }

    @Bean
    public IniciarPrueba iniciarPrueba(NegocioService negocioService,
                                       PruebaPremiumService pruebaPremiumService,
                                       Clock clock) {
        return new IniciarPrueba(negocioService, pruebaPremiumService, clock);
    }

    @Bean
    public VencerPruebas vencerPruebas(PruebaPremiumService pruebaPremiumService, Clock clock) {
        return new VencerPruebas(pruebaPremiumService, clock);
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
