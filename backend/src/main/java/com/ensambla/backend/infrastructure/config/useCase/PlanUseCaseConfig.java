package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.useCase.plan.listarCatalogoPlanes.ListarCatalogoPlanes;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.AccesoContenidoRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
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
    public ListarCatalogoPlanes listarCatalogoPlanes(PlanRepository planRepository, TransaccionPort transaccionPort) {
        return new ListarCatalogoPlanes(planRepository, transaccionPort);
    }

    @Bean
    public DesbloquearContenidos desbloquearContenidos(
            NegocioRepository negocioRepository, ControlPlanService controlPlanService, Clock clock) {
        return new DesbloquearContenidos(negocioRepository, controlPlanService, clock);
    }

    @Bean
    public ConsultarPlanes consultarPlanes(NegocioRepository negocioRepository,
                                           SuscripcionRepository suscripcionRepository,
                                           PlanRepository planRepository,
                                           PruebaPremiumRepository pruebaPremiumRepository,
                                           UsoPlanRepository usoPlanRepository,
                                           RenovacionPremiumRepository renovacionPremiumRepository,
                                           TransaccionPort transaccionPort,
                                           Clock clock) {
        return new ConsultarPlanes(negocioRepository, suscripcionRepository, planRepository, pruebaPremiumRepository,
                usoPlanRepository, renovacionPremiumRepository, transaccionPort, clock);
    }

    @Bean
    public IniciarPrueba iniciarPrueba(NegocioRepository negocioRepository,
                                       PlanRepository planRepository,
                                       PruebaPremiumRepository pruebaPremiumRepository,
                                       SuscripcionRepository suscripcionRepository,
                                       UsoPlanRepository usoPlanRepository,
                                       RenovacionPremiumRepository renovacionPremiumRepository,
                                       TransaccionPort transaccionPort,
                                       Clock clock) {
        return new IniciarPrueba(negocioRepository, planRepository, pruebaPremiumRepository, suscripcionRepository,
                usoPlanRepository, renovacionPremiumRepository, transaccionPort, clock);
    }

    @Bean
    public VencerPruebas vencerPruebas(PlanRepository planRepository,
                                       PruebaPremiumRepository pruebaPremiumRepository,
                                       SuscripcionRepository suscripcionRepository,
                                       UsoPlanRepository usoPlanRepository,
                                       TransaccionPort transaccionPort,
                                       Clock clock) {
        return new VencerPruebas(planRepository, pruebaPremiumRepository, suscripcionRepository,
                usoPlanRepository, transaccionPort, clock);
    }

    @Bean
    public ControlPlanService controlPlanService(
            PlanRepository planRepository,
            SuscripcionRepository suscripcionRepository,
            UsoPlanRepository usoPlanRepository,
            PruebaPremiumRepository pruebaPremiumRepository,
            AccesoContenidoRepository accesoContenidoRepository,
            TransaccionPort transaccionPort) {
        return new ControlPlanService(planRepository, suscripcionRepository, usoPlanRepository,
                accesoContenidoRepository, pruebaPremiumRepository, transaccionPort);
    }

}
