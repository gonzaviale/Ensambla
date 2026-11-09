package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.ExportadorReportePort;
import com.ensambla.backend.domain.port.GeneradorResumenPdfPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.service.reporte.ReporteService;
import com.ensambla.backend.domain.useCase.reporte.exportarReporte.ExportarReporte;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenNegocio;
import com.ensambla.backend.domain.useCase.reporte.generarReporte.GenerarReporte;
import com.ensambla.backend.domain.useCase.reporte.obtenerMetricas.ObtenerMetricas;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicio;
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
    public ExportarResumenNegocio exportarResumenNegocio(
            NegocioRepository negocioRepository,
            ControlPlanService controlPlanService,
            ProyectoRepository proyectoRepository,
            DatosDeProyectoService datosDeProyectoService,
            ZonaRepository zonaRepository,
            AgregadoZonaRepository agregadoZonaRepository,
            AvisoRepository avisoRepository,
            GeneradorResumenPdfPort generadorResumenPdfPort,
            Clock clock) {
        return new ExportarResumenNegocio(negocioRepository, controlPlanService, proyectoRepository,
                datosDeProyectoService, zonaRepository, agregadoZonaRepository, avisoRepository,
                generadorResumenPdfPort, clock);
    }

    @Bean
    public ObtenerResumenInicio obtenerResumenInicio(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository,
            DatosDeProyectoService datosDeProyectoService,
            Clock clock) {
        return new ObtenerResumenInicio(negocioRepository, proyectoRepository, datosDeProyectoService, clock);
    }

    @Bean
    public ObtenerMetricas obtenerMetricas(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository,
            DatosDeProyectoService datosDeProyectoService,
            ZonaRepository zonaRepository,
            AgregadoZonaRepository agregadoZonaRepository,
            AvisoRepository avisoRepository,
            Clock clock) {
        return new ObtenerMetricas(negocioRepository, proyectoRepository, datosDeProyectoService, zonaRepository,
                agregadoZonaRepository, avisoRepository, clock);
    }

    @Bean
    public DatosDeProyectoService datosDeProyectoService(RegistroRepository registroRepository) {
        return new DatosDeProyectoService(registroRepository);
    }

    @Bean
    public ReporteService reporteService(RegistroRepository registroRepository) {
        return new ReporteService(registroRepository);
    }
}
