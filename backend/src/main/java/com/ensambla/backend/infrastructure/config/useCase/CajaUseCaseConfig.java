package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVenta;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCaja;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCaja;
import com.ensambla.backend.domain.useCase.caja.obtenerCierre.ObtenerCierre;
import com.ensambla.backend.domain.repository.caja.CierreCajaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CajaUseCaseConfig {

    @Bean
    public ConsultarCaja consultarCaja(NegocioRepository negocioRepository, SistemaRepository sistemaRepository,
            VentaCajaRepository ventaCajaRepository, CierreCajaRepository cierreCajaRepository,
            UsoPlanRepository usoPlanRepository, TransaccionPort transaccionPort, Clock clock) {
        return new ConsultarCaja(negocioRepository, sistemaRepository, ventaCajaRepository, cierreCajaRepository,
                usoPlanRepository, transaccionPort, clock);
    }

    @Bean
    public CerrarCaja cerrarCaja(NegocioRepository negocioRepository, SistemaRepository sistemaRepository,
            VentaCajaRepository ventaCajaRepository, CierreCajaRepository cierreCajaRepository,
            UsoPlanRepository usoPlanRepository, TransaccionPort transaccionPort, Clock clock) {
        return new CerrarCaja(negocioRepository, sistemaRepository, ventaCajaRepository, cierreCajaRepository,
                usoPlanRepository, transaccionPort, clock);
    }

    @Bean
    public ObtenerCierre obtenerCierre(NegocioRepository negocioRepository, CierreCajaRepository cierreCajaRepository) {
        return new ObtenerCierre(negocioRepository, cierreCajaRepository);
    }

    @Bean
    public CobrarVenta cobrarVenta(NegocioRepository negocioRepository, UsoPlanRepository usoPlanRepository,
            SistemaRepository sistemaRepository, VentaCajaRepository ventaCajaRepository,
            ControlPlanService controlPlanService, TransaccionPort transaccionPort, Clock clock) {
        return new CobrarVenta(negocioRepository, usoPlanRepository, sistemaRepository, ventaCajaRepository,
                controlPlanService, transaccionPort, clock);
    }
}
