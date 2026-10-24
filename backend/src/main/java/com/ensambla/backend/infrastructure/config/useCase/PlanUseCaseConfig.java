package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.AccesoContenidoRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
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
                                           SuscripcionService suscripcionService,
                                           PlanRepository planRepository,
                                           PruebaPremiumRepository pruebaPremiumRepository,
                                           UsoPlanRepository usoPlanRepository,
                                           RenovacionPremiumRepository renovacionPremiumRepository,
                                           TransaccionPort transaccionPort,
                                           Clock clock) {
        return new ConsultarPlanes(negocioService, suscripcionService, planRepository, pruebaPremiumRepository,
                usoPlanRepository, renovacionPremiumRepository, transaccionPort, clock);
    }

    @Bean
    public IniciarPrueba iniciarPrueba(NegocioService negocioService,
                                       SuscripcionService suscripcionService,
                                       PruebaPremiumRepository pruebaPremiumRepository,
                                       SuscripcionRepository suscripcionRepository,
                                       UsoPlanRepository usoPlanRepository,
                                       RenovacionPremiumRepository renovacionPremiumRepository,
                                       TransaccionPort transaccionPort,
                                       Clock clock) {
        return new IniciarPrueba(negocioService, suscripcionService, pruebaPremiumRepository, suscripcionRepository,
                usoPlanRepository, renovacionPremiumRepository, transaccionPort, clock);
    }

    @Bean
    public VencerPruebas vencerPruebas(SuscripcionService suscripcionService,
                                       PruebaPremiumRepository pruebaPremiumRepository,
                                       SuscripcionRepository suscripcionRepository,
                                       UsoPlanRepository usoPlanRepository,
                                       TransaccionPort transaccionPort,
                                       Clock clock) {
        return new VencerPruebas(suscripcionService, pruebaPremiumRepository, suscripcionRepository,
                usoPlanRepository, transaccionPort, clock);
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
    public SuscripcionService suscripcionService(
            PlanRepository planRepository, SuscripcionRepository suscripcionRepository) {
        return new SuscripcionService(planRepository, suscripcionRepository);
    }
}
