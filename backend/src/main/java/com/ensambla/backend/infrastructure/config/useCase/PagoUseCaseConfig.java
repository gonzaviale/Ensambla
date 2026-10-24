package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.pago.ConsultarRenovacionMapper;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pago.PagoPremiumRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.service.usuario.UsuarioService;
import com.ensambla.backend.domain.useCase.pago.ActualizarRenovacionUseCase;
import com.ensambla.backend.domain.useCase.pago.CancelarRenovacionUseCase;
import com.ensambla.backend.domain.useCase.pago.ConciliarPagosUseCase;
import com.ensambla.backend.domain.useCase.pago.ConsultarRenovacionUseCase;
import com.ensambla.backend.domain.useCase.pago.ContratarPremiumUseCase;
import com.ensambla.backend.domain.useCase.pago.RecibirNotificacionPagoUseCase;
import com.ensambla.backend.domain.useCase.pago.SincronizarRenovacionUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PagoUseCaseConfig {

    @Bean
    public ConsultarRenovacionMapper consultarRenovacionMapper() {
        return new ConsultarRenovacionMapper();
    }

    @Bean
    public SincronizarRenovacionUseCase sincronizarRenovacionUseCase(RenovacionPremiumService renovacionPremiumService,
            PasarelaSuscripcionesPort pasarelaSuscripcionesPort, Clock clock) {
        return new SincronizarRenovacionUseCase(renovacionPremiumService, pasarelaSuscripcionesPort, clock);
    }

    @Bean
    public ConsultarRenovacionUseCase consultarRenovacionUseCase(NegocioService negocioService,
            RenovacionPremiumService renovacionPremiumService, PasarelaSuscripcionesPort pasarelaSuscripcionesPort,
            ConsultarRenovacionMapper consultarRenovacionMapper) {
        return new ConsultarRenovacionUseCase(negocioService, renovacionPremiumService, pasarelaSuscripcionesPort,
                consultarRenovacionMapper);
    }

    @Bean
    public ContratarPremiumUseCase contratarPremiumUseCase(NegocioService negocioService, UsuarioService usuarioService,
            RenovacionPremiumService renovacionPremiumService, PasarelaSuscripcionesPort pasarelaSuscripcionesPort,
            ConsultarRenovacionMapper consultarRenovacionMapper, SincronizarRenovacionUseCase sincronizarRenovacionUseCase,
            Clock clock) {
        return new ContratarPremiumUseCase(negocioService, usuarioService, renovacionPremiumService,
                pasarelaSuscripcionesPort, consultarRenovacionMapper, sincronizarRenovacionUseCase, clock);
    }

    @Bean
    public CancelarRenovacionUseCase cancelarRenovacionUseCase(NegocioService negocioService,
            RenovacionPremiumService renovacionPremiumService, PasarelaSuscripcionesPort pasarelaSuscripcionesPort,
            ConsultarRenovacionMapper consultarRenovacionMapper, SincronizarRenovacionUseCase sincronizarRenovacionUseCase,
            Clock clock) {
        return new CancelarRenovacionUseCase(negocioService, renovacionPremiumService, pasarelaSuscripcionesPort,
                consultarRenovacionMapper, sincronizarRenovacionUseCase, clock);
    }

    @Bean
    public ActualizarRenovacionUseCase actualizarRenovacionUseCase(NegocioService negocioService,
            RenovacionPremiumService renovacionPremiumService, SincronizarRenovacionUseCase sincronizarRenovacionUseCase) {
        return new ActualizarRenovacionUseCase(negocioService, renovacionPremiumService, sincronizarRenovacionUseCase);
    }

    @Bean
    public RecibirNotificacionPagoUseCase recibirNotificacionPagoUseCase(PasarelaSuscripcionesPort pasarelaSuscripcionesPort,
            RenovacionPremiumService renovacionPremiumService, SincronizarRenovacionUseCase sincronizarRenovacionUseCase) {
        return new RecibirNotificacionPagoUseCase(pasarelaSuscripcionesPort, renovacionPremiumService,
                sincronizarRenovacionUseCase);
    }

    @Bean
    public ConciliarPagosUseCase conciliarPagosUseCase(RenovacionPremiumService renovacionPremiumService,
            SincronizarRenovacionUseCase sincronizarRenovacionUseCase, Clock clock) {
        return new ConciliarPagosUseCase(renovacionPremiumService, sincronizarRenovacionUseCase, clock);
    }

    @Bean
    public RenovacionPremiumService renovacionPremiumService(
            RenovacionPremiumRepository renovacionPremiumRepository,
            PagoPremiumRepository pagoPremiumRepository,
            UsoPlanRepository usoPlanRepository,
            SuscripcionRepository suscripcionRepository,
            PlanRepository planRepository,
            TransaccionPort transaccionPort) {
        return new RenovacionPremiumService(
                renovacionPremiumRepository, pagoPremiumRepository, usoPlanRepository, suscripcionRepository, planRepository, transaccionPort);
    }
}
