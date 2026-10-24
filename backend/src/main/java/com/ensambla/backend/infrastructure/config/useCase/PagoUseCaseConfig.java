package com.ensambla.backend.infrastructure.config.useCase;

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
import com.ensambla.backend.domain.useCase.pago.actualizarRenovacion.ActualizarRenovacion;
import com.ensambla.backend.domain.useCase.pago.cancelarRenovacion.CancelarRenovacion;
import com.ensambla.backend.domain.useCase.pago.conciliarPagos.ConciliarPagos;
import com.ensambla.backend.domain.useCase.pago.consultarRenovacion.ConsultarRenovacion;
import com.ensambla.backend.domain.useCase.pago.contratarPremium.ContratarPremium;
import com.ensambla.backend.domain.useCase.pago.recibirNotificacionPago.RecibirNotificacionPago;
import com.ensambla.backend.domain.useCase.pago.sincronizarRenovacion.SincronizarRenovacion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PagoUseCaseConfig {

    @Bean
    public SincronizarRenovacion sincronizarRenovacion(RenovacionPremiumService renovacionPremiumService,
            PasarelaSuscripcionesPort pasarelaSuscripcionesPort, Clock clock) {
        return new SincronizarRenovacion(renovacionPremiumService, pasarelaSuscripcionesPort, clock);
    }

    @Bean
    public ConsultarRenovacion consultarRenovacion(NegocioService negocioService,
            RenovacionPremiumService renovacionPremiumService, PasarelaSuscripcionesPort pasarelaSuscripcionesPort) {
        return new ConsultarRenovacion(negocioService, renovacionPremiumService, pasarelaSuscripcionesPort);
    }

    @Bean
    public ContratarPremium contratarPremium(NegocioService negocioService, UsuarioService usuarioService,
            RenovacionPremiumService renovacionPremiumService, PasarelaSuscripcionesPort pasarelaSuscripcionesPort,
            SincronizarRenovacion sincronizarRenovacion,
            Clock clock) {
        return new ContratarPremium(negocioService, usuarioService, renovacionPremiumService,
                pasarelaSuscripcionesPort, sincronizarRenovacion, clock);
    }

    @Bean
    public CancelarRenovacion cancelarRenovacion(NegocioService negocioService,
            RenovacionPremiumService renovacionPremiumService, PasarelaSuscripcionesPort pasarelaSuscripcionesPort,
            SincronizarRenovacion sincronizarRenovacion,
            Clock clock) {
        return new CancelarRenovacion(
                negocioService, renovacionPremiumService, pasarelaSuscripcionesPort, sincronizarRenovacion, clock);
    }

    @Bean
    public ActualizarRenovacion actualizarRenovacion(NegocioService negocioService,
            RenovacionPremiumService renovacionPremiumService, SincronizarRenovacion sincronizarRenovacion) {
        return new ActualizarRenovacion(negocioService, renovacionPremiumService, sincronizarRenovacion);
    }

    @Bean
    public RecibirNotificacionPago recibirNotificacionPago(PasarelaSuscripcionesPort pasarelaSuscripcionesPort,
            RenovacionPremiumService renovacionPremiumService, SincronizarRenovacion sincronizarRenovacion) {
        return new RecibirNotificacionPago(pasarelaSuscripcionesPort, renovacionPremiumService,
                sincronizarRenovacion);
    }

    @Bean
    public ConciliarPagos conciliarPagos(RenovacionPremiumService renovacionPremiumService,
            SincronizarRenovacion sincronizarRenovacion, Clock clock) {
        return new ConciliarPagos(renovacionPremiumService, sincronizarRenovacion, clock);
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
