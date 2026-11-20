package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.useCase.caja.cancelarCobroQr.CancelarCobroQr;
import com.ensambla.backend.domain.useCase.caja.generarCobroQr.GenerarCobroQr;
import com.ensambla.backend.domain.repository.caja.CobroQrRepository;
import com.ensambla.backend.domain.service.caja.CobroQrService;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
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
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.useCase.caja.listarVentas.ListarVentasCaja;
import com.ensambla.backend.domain.useCase.caja.registrarCobro.RegistrarCobro;

@Configuration
public class CajaUseCaseConfig {

    @Bean
    public ListarVentasCaja listarVentasCaja(NegocioRepository negocioRepository, SistemaRepository sistemaRepository,
            VentaCajaRepository ventaCajaRepository, CobroCajaRepository cobroCajaRepository) {
        return new ListarVentasCaja(negocioRepository, sistemaRepository, ventaCajaRepository, cobroCajaRepository);
    }

    @Bean
    public RegistrarCobro registrarCobro(NegocioRepository negocioRepository, UsoPlanRepository usoPlanRepository,
            VentaCajaRepository ventaCajaRepository, CobroCajaRepository cobroCajaRepository,
            CobroQrService cobroQrService, TransaccionPort transaccionPort, Clock clock) {
        return new RegistrarCobro(negocioRepository, usoPlanRepository, ventaCajaRepository, cobroCajaRepository,
                cobroQrService, transaccionPort, clock);
    }

    @Bean
    public ConsultarCaja consultarCaja(NegocioRepository negocioRepository, SistemaRepository sistemaRepository,
            VentaCajaRepository ventaCajaRepository, CierreCajaRepository cierreCajaRepository,
            CobroCajaRepository cobroCajaRepository,
            UsoPlanRepository usoPlanRepository, TransaccionPort transaccionPort, Clock clock) {
        return new ConsultarCaja(negocioRepository, sistemaRepository, ventaCajaRepository, cierreCajaRepository,
                cobroCajaRepository, usoPlanRepository, transaccionPort, clock);
    }

    @Bean
    public CerrarCaja cerrarCaja(NegocioRepository negocioRepository, SistemaRepository sistemaRepository,
            VentaCajaRepository ventaCajaRepository, CierreCajaRepository cierreCajaRepository,
            CobroCajaRepository cobroCajaRepository,
            UsoPlanRepository usoPlanRepository, TransaccionPort transaccionPort, Clock clock) {
        return new CerrarCaja(negocioRepository, sistemaRepository, ventaCajaRepository, cierreCajaRepository,
                cobroCajaRepository, usoPlanRepository, transaccionPort, clock);
    }

    @Bean
    public ObtenerCierre obtenerCierre(NegocioRepository negocioRepository, CierreCajaRepository cierreCajaRepository) {
        return new ObtenerCierre(negocioRepository, cierreCajaRepository);
    }

    @Bean
    public CobrarVenta cobrarVenta(NegocioRepository negocioRepository, UsoPlanRepository usoPlanRepository,
            SistemaRepository sistemaRepository, VentaCajaRepository ventaCajaRepository,
            MovimientoStockRepository movimientoStockRepository, ControlPlanService controlPlanService,
            CobroQrService cobroQrService, TransaccionPort transaccionPort, Clock clock) {
        return new CobrarVenta(negocioRepository, usoPlanRepository, sistemaRepository, ventaCajaRepository,
                movimientoStockRepository, controlPlanService, cobroQrService, transaccionPort, clock);
    }

    @Bean
    public CobroQrService cobroQrService(CobroQrRepository cobroQrRepository) {
        return new CobroQrService(cobroQrRepository);
    }

    @Bean
    public GenerarCobroQr generarCobroQr(NegocioRepository negocioRepository, CobroQrRepository cobroQrRepository,
            TransaccionPort transaccionPort, Clock clock) {
        return new GenerarCobroQr(negocioRepository, cobroQrRepository, transaccionPort, clock);
    }

    @Bean
    public CancelarCobroQr cancelarCobroQr(NegocioRepository negocioRepository, UsoPlanRepository usoPlanRepository,
            CobroQrRepository cobroQrRepository, TransaccionPort transaccionPort, Clock clock) {
        return new CancelarCobroQr(negocioRepository, usoPlanRepository, cobroQrRepository, transaccionPort, clock);
    }
}
