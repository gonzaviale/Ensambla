package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.reporte.ExportarReporteMapper;
import com.ensambla.backend.domain.mapper.reporte.GenerarReporteMapper;
import com.ensambla.backend.domain.port.ExportadorReportePort;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.ReporteService;
import com.ensambla.backend.domain.useCase.reporte.ExportarReporteUseCase;
import com.ensambla.backend.domain.useCase.reporte.GenerarReporteUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ReporteUseCaseConfig {

    @Bean
    public GenerarReporteMapper generarReporteMapper() {
        return new GenerarReporteMapper();
    }

    @Bean
    public ExportarReporteMapper exportarReporteMapper() {
        return new ExportarReporteMapper();
    }

    @Bean
    public GenerarReporteUseCase generarReporteUseCase(NegocioService negocioService,
                                                       ControlPlanService controlPlanService,
                                                       Clock clock,
                                                       BloqueService bloqueService,
                                                       CampoService campoService,
                                                       ReporteService reporteService,
                                                       GenerarReporteMapper generarReporteMapper) {
        return new GenerarReporteUseCase(
                negocioService, controlPlanService, clock, bloqueService, campoService, reporteService,
                generarReporteMapper);
    }

    @Bean
    public ExportarReporteUseCase exportarReporteUseCase(NegocioService negocioService,
                                                         ControlPlanService controlPlanService,
                                                         BloqueService bloqueService,
                                                         CampoService campoService,
                                                         ReporteService reporteService,
                                                         ExportadorReportePort exportadorReportePort,
                                                         ExportarReporteMapper exportarReporteMapper,
                                                         Clock clock) {
        return new ExportarReporteUseCase(
                negocioService, controlPlanService, bloqueService, campoService, reporteService,
                exportadorReportePort, exportarReporteMapper, clock);
    }
}
