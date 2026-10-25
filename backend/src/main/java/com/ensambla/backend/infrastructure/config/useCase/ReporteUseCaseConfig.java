package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.ExportadorReportePort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.ReporteService;
import com.ensambla.backend.domain.useCase.reporte.exportarReporte.ExportarReporte;
import com.ensambla.backend.domain.useCase.reporte.generarReporte.GenerarReporte;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ReporteUseCaseConfig {

    @Bean
    public GenerarReporte generarReporte(NegocioRepository negocioRepository,
                                         ControlPlanService controlPlanService,
                                         Clock clock,
                                         BloqueRepository bloqueRepository,
                                         CampoRepository campoRepository,
                                         ReporteService reporteService) {
        return new GenerarReporte(
                negocioRepository, controlPlanService, clock, bloqueRepository, campoRepository, reporteService);
    }

    @Bean
    public ExportarReporte exportarReporte(
            NegocioRepository negocioRepository,
            ControlPlanService controlPlanService,
            BloqueRepository bloqueRepository,
            ReporteService reporteService,
            RegistroRepository registroRepository,
            CampoRepository campoRepository,
            ExportadorReportePort exportadorReportePort,
            Clock clock) {
        return new ExportarReporte(
                negocioRepository, controlPlanService, bloqueRepository, reporteService, registroRepository,
                campoRepository, exportadorReportePort, clock);
    }

    @Bean
    public ReporteService reporteService(RegistroRepository registroRepository) {
        return new ReporteService(registroRepository);
    }
}
