package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pago.PagoPremiumRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.useCase.pago.actualizarRenovacion.ActualizarRenovacion;
import com.ensambla.backend.domain.useCase.pago.cancelarRenovacion.CancelarRenovacion;
import com.ensambla.backend.domain.useCase.pago.conciliarPagos.ConciliarPagos;
import com.ensambla.backend.domain.useCase.pago.consultarRenovacion.ConsultarRenovacion;
import com.ensambla.backend.domain.useCase.pago.contratarPremium.ContratarPremium;
import com.ensambla.backend.domain.useCase.pago.recibirNotificacionPago.RecibirNotificacionPago;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PagoUseCaseConfig {

    @Bean
    public ConsultarRenovacion consultarRenovacion(NegocioRepository negocioRepository,
            RenovacionPremiumService renovacionPremiumService, PasarelaSuscripcionesPort pasarelaSuscripcionesPort) {
        return new ConsultarRenovacion(negocioRepository, renovacionPremiumService, pasarelaSuscripcionesPort);
    }

    @Bean
    public ContratarPremium contratarPremium(
            NegocioRepository negocioRepository,
            UsuarioRepository usuarioRepository,
            RenovacionPremiumService renovacionPremiumService,
            PasarelaSuscripcionesPort pasarelaSuscripcionesPort,
            SuscripcionRepository suscripcionRepository,
            PlanRepository planRepository,
            RenovacionPremiumRepository renovacionPremiumRepository,
            UsoPlanRepository usoPlanRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new ContratarPremium(negocioRepository, usuarioRepository, renovacionPremiumService,
                pasarelaSuscripcionesPort, suscripcionRepository, planRepository, renovacionPremiumRepository,
                usoPlanRepository, transaccionPort, clock);
    }

    @Bean
    public CancelarRenovacion cancelarRenovacion(
            NegocioRepository negocioRepository,
            RenovacionPremiumService renovacionPremiumService,
            PasarelaSuscripcionesPort pasarelaSuscripcionesPort,
            RenovacionPremiumRepository renovacionPremiumRepository,
            UsoPlanRepository usoPlanRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new CancelarRenovacion(negocioRepository, renovacionPremiumService, pasarelaSuscripcionesPort,
                renovacionPremiumRepository, usoPlanRepository, transaccionPort, clock);
    }

    @Bean
    public ActualizarRenovacion actualizarRenovacion(NegocioRepository negocioRepository,
            RenovacionPremiumService renovacionPremiumService, Clock clock) {
        return new ActualizarRenovacion(negocioRepository, renovacionPremiumService, clock);
    }

    @Bean
    public RecibirNotificacionPago recibirNotificacionPago(PasarelaSuscripcionesPort pasarelaSuscripcionesPort,
            RenovacionPremiumRepository renovacionPremiumRepository, RenovacionPremiumService renovacionPremiumService,
            Clock clock) {
        return new RecibirNotificacionPago(pasarelaSuscripcionesPort, renovacionPremiumRepository,
                renovacionPremiumService, clock);
    }

    @Bean
    public ConciliarPagos conciliarPagos(
            RenovacionPremiumService renovacionPremiumService,
            RenovacionPremiumRepository renovacionPremiumRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new ConciliarPagos(renovacionPremiumService, renovacionPremiumRepository,
                transaccionPort, clock);
    }

    @Bean
    public RenovacionPremiumService renovacionPremiumService(
            RenovacionPremiumRepository renovacionPremiumRepository,
            UsoPlanRepository usoPlanRepository,
            PasarelaSuscripcionesPort pasarelaSuscripcionesPort,
            PagoPremiumRepository pagoPremiumRepository,
            PlanRepository planRepository,
            SuscripcionRepository suscripcionRepository,
            TransaccionPort transaccionPort) {
        return new RenovacionPremiumService(renovacionPremiumRepository, usoPlanRepository, pasarelaSuscripcionesPort,
                pagoPremiumRepository, planRepository, suscripcionRepository, transaccionPort);
    }
}
