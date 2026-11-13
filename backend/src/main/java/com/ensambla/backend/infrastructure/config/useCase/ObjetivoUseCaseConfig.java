package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.port.RedaccionObjetivosIaPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ProgresoObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ResumenSemanalRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.objetivo.DecisionesObjetivoService;
import com.ensambla.backend.domain.service.objetivo.MonitoreoObjetivosService;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.objetivo.abandonarMiniObjetivo.AbandonarMiniObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.cerrarSemanasObjetivos.CerrarSemanasObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.crearObjetivo.CrearObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.eliminarObjetivo.EliminarObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatos;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisiones;
import com.ensambla.backend.domain.useCase.objetivo.listarObjetivos.ListarObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanas;
import com.ensambla.backend.domain.useCase.objetivo.medirObjetivos.MedirObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.monitorearObjetivos.MonitorearObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgreso;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasos;
import com.ensambla.backend.domain.useCase.objetivo.registrarDecisionPaso.RegistrarDecisionPaso;
import com.ensambla.backend.domain.useCase.objetivo.reemplazarMiniObjetivo.ReemplazarMiniObjetivo;
import com.ensambla.backend.infrastructure.ia.ObjetivosIaProperties;
import com.ensambla.backend.infrastructure.objetivos.ObjetivosCierreProperties;
import com.ensambla.backend.infrastructure.objetivos.ObjetivosMonitoreoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({ObjetivosMonitoreoProperties.class, ObjetivosCierreProperties.class})
public class ObjetivoUseCaseConfig {

    @Bean
    public ListarObjetivos listarObjetivos(
            NegocioRepository negocioRepository,
            ObjetivoRepository objetivoRepository) {
        return new ListarObjetivos(
                negocioRepository,
                objetivoRepository);
    }

    @Bean
    public PropuestaMiniObjetivosService propuestaMiniObjetivosService(
            NegocioRepository negocioRepository,
            SistemaRepository sistemaRepository,
            DatosDeProyectoService datosDeProyectoService,
            ZonaRepository zonaRepository,
            AgregadoZonaRepository agregadoZonaRepository,
            MetricasVentas metricasVentas) {
        return new PropuestaMiniObjetivosService(
                negocioRepository,
                sistemaRepository,
                datosDeProyectoService,
                zonaRepository,
                agregadoZonaRepository,
                metricasVentas);
    }

    @Bean
    public CrearObjetivo crearObjetivo(
            NegocioRepository negocioRepository,
            ObjetivoRepository objetivoRepository,
            MiniObjetivoRepository miniObjetivoRepository,
            PropuestaMiniObjetivosService propuestaMiniObjetivosService,
            ControlPlanService controlPlanService,
            ConsumoSamiRepository consumoSamiRepository,
            RedaccionObjetivosIaPort redaccionObjetivosIaPort,
            TransaccionPort transaccionPort,
            Clock clock,
            ObjetivosIaProperties objetivosIaProperties) {
        return new CrearObjetivo(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                propuestaMiniObjetivosService,
                controlPlanService,
                consumoSamiRepository,
                redaccionObjetivosIaPort,
                transaccionPort,
                clock,
                objetivosIaProperties.habilitada(),
                objetivosIaProperties.limiteDiario());
    }

    @Bean
    public ListarMiniObjetivos listarMiniObjetivos(
            NegocioRepository negocioRepository,
            ObjetivoRepository objetivoRepository,
            MiniObjetivoRepository miniObjetivoRepository) {
        return new ListarMiniObjetivos(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository);
    }

    @Bean
    public ListarCandidatos listarCandidatos(
            NegocioRepository negocioRepository,
            ObjetivoRepository objetivoRepository,
            MiniObjetivoRepository miniObjetivoRepository,
            PropuestaMiniObjetivosService propuestaMiniObjetivosService,
            Clock clock) {
        return new ListarCandidatos(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                propuestaMiniObjetivosService,
                clock);
    }

    @Bean
    public ReemplazarMiniObjetivo reemplazarMiniObjetivo(
            NegocioRepository negocioRepository,
            ObjetivoRepository objetivoRepository,
            MiniObjetivoRepository miniObjetivoRepository,
            PropuestaMiniObjetivosService propuestaMiniObjetivosService,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new ReemplazarMiniObjetivo(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                propuestaMiniObjetivosService,
                transaccionPort,
                clock);
    }

    @Bean
    public AbandonarMiniObjetivo abandonarMiniObjetivo(
            NegocioRepository negocioRepository,
            ObjetivoRepository objetivoRepository,
            MiniObjetivoRepository miniObjetivoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new AbandonarMiniObjetivo(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                transaccionPort,
                clock);
    }

    @Bean
    public MonitoreoObjetivosService monitoreoObjetivosService(
            ObjetivoRepository objetivoRepository,
            MiniObjetivoRepository miniObjetivoRepository,
            ProgresoObjetivoRepository progresoObjetivoRepository,
            PropuestaMiniObjetivosService propuestaMiniObjetivosService) {
        return new MonitoreoObjetivosService(
                objetivoRepository,
                miniObjetivoRepository,
                progresoObjetivoRepository,
                propuestaMiniObjetivosService);
    }

    @Bean
    public MonitorearObjetivos monitorearObjetivos(
            ObjetivoRepository objetivoRepository,
            MonitoreoObjetivosService monitoreoObjetivosService,
            TransaccionPort transaccionPort,
            Clock clock,
            ObjetivosMonitoreoProperties objetivosMonitoreoProperties) {
        return new MonitorearObjetivos(
                objetivoRepository,
                monitoreoObjetivosService,
                transaccionPort,
                clock,
                objetivosMonitoreoProperties.tamanoTanda());
    }

    @Bean
    public MedirObjetivos medirObjetivos(
            NegocioRepository negocioRepository,
            MonitoreoObjetivosService monitoreoObjetivosService,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new MedirObjetivos(negocioRepository, monitoreoObjetivosService, transaccionPort, clock);
    }

    @Bean
    public ObtenerProgreso obtenerProgreso(
            NegocioRepository negocioRepository,
            ObjetivoRepository objetivoRepository,
            MiniObjetivoRepository miniObjetivoRepository,
            ProgresoObjetivoRepository progresoObjetivoRepository,
            PropuestaMiniObjetivosService propuestaMiniObjetivosService,
            ResumenSemanalRepository resumenSemanalRepository,
            Clock clock) {
        return new ObtenerProgreso(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                progresoObjetivoRepository,
                propuestaMiniObjetivosService,
                resumenSemanalRepository,
                clock);
    }

    @Bean
    public DecisionesObjetivoService decisionesObjetivoService(
            ObjetivoRepository objetivoRepository,
            MiniObjetivoRepository miniObjetivoRepository,
            DecisionObjetivoRepository decisionObjetivoRepository) {
        return new DecisionesObjetivoService(
                objetivoRepository,
                miniObjetivoRepository,
                decisionObjetivoRepository);
    }

    @Bean
    public RegistrarDecisionPaso registrarDecisionPaso(
            NegocioRepository negocioRepository,
            ObjetivoRepository objetivoRepository,
            MiniObjetivoRepository miniObjetivoRepository,
            DecisionObjetivoRepository decisionObjetivoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new RegistrarDecisionPaso(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                decisionObjetivoRepository,
                transaccionPort,
                clock);
    }

    @Bean
    public ListarDecisiones listarDecisiones(
            NegocioRepository negocioRepository,
            ObjetivoRepository objetivoRepository,
            DecisionObjetivoRepository decisionObjetivoRepository,
            AvisoRepository avisoRepository) {
        return new ListarDecisiones(
                negocioRepository,
                objetivoRepository,
                decisionObjetivoRepository,
                avisoRepository);
    }

    @Bean
    public CerrarSemanasObjetivos cerrarSemanasObjetivos(
            ObjetivoRepository objetivoRepository,
            MiniObjetivoRepository miniObjetivoRepository,
            DecisionObjetivoRepository decisionObjetivoRepository,
            AvisoRepository avisoRepository,
            ResumenSemanalRepository resumenSemanalRepository,
            PropuestaMiniObjetivosService propuestaMiniObjetivosService,
            TransaccionPort transaccionPort,
            Clock clock,
            ObjetivosCierreProperties objetivosCierreProperties) {
        return new CerrarSemanasObjetivos(
                objetivoRepository,
                miniObjetivoRepository,
                decisionObjetivoRepository,
                avisoRepository,
                resumenSemanalRepository,
                propuestaMiniObjetivosService,
                transaccionPort,
                clock,
                objetivosCierreProperties.maxSemanasPendientes(),
                objetivosCierreProperties.tamanoTanda());
    }

    @Bean
    public ListarSemanas listarSemanas(
            NegocioRepository negocioRepository,
            ObjetivoRepository objetivoRepository,
            ResumenSemanalRepository resumenSemanalRepository) {
        return new ListarSemanas(
                negocioRepository,
                objetivoRepository,
                resumenSemanalRepository);
    }

    @Bean
    public EliminarObjetivo eliminarObjetivo(
            NegocioRepository negocioRepository,
            ObjetivoRepository objetivoRepository,
            TransaccionPort transaccionPort) {
        return new EliminarObjetivo(
                negocioRepository,
                objetivoRepository,
                transaccionPort);
    }

    @Bean
    public ObtenerPasos obtenerPasos(
            NegocioRepository negocioRepository,
            SistemaRepository sistemaRepository,
            DatosDeProyectoService datosDeProyectoService,
            ZonaRepository zonaRepository,
            AgregadoZonaRepository agregadoZonaRepository,
            MetricasVentas metricasVentas,
            Clock clock) {
        return new ObtenerPasos(
                negocioRepository,
                sistemaRepository,
                datosDeProyectoService,
                zonaRepository,
                agregadoZonaRepository,
                metricasVentas,
                clock);
    }
}
