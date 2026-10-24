package com.ensambla.backend.infrastructure.config.useCase;


import com.ensambla.backend.domain.port.ExportadorReportePort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
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
    public GenerarReporte generarReporte(NegocioService negocioService,
                                         ControlPlanService controlPlanService,
                                         Clock clock,
                                         BloqueService bloqueService,
                                         CampoService campoService,
                                         ReporteService reporteService) {
        return new GenerarReporte(
                negocioService, controlPlanService, clock, bloqueService, campoService, reporteService);
    }

    @Bean
    public ExportarReporte exportarReporte(
            NegocioService negocioService,
            ControlPlanService controlPlanService,
            BloqueService bloqueService,
            CampoService campoService,
            ReporteService reporteService,
            RegistroRepository registroRepository,
            CampoRepository campoRepository,
            ExportadorReportePort exportadorReportePort,
            Clock clock) {
        return new ExportarReporte(
                negocioService, controlPlanService, bloqueService, campoService, reporteService, registroRepository,
                campoRepository, exportadorReportePort, clock);
    }

    @Bean
    public ReporteService reporteService(RegistroRepository registroRepository) {
        return new ReporteService(registroRepository);
    }
}
