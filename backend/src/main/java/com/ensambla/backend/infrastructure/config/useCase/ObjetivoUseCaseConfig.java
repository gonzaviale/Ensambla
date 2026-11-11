package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.port.RedaccionObjetivosIaPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.objetivo.abandonarMiniObjetivo.AbandonarMiniObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.crearObjetivo.CrearObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.eliminarObjetivo.EliminarObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatos;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.listarObjetivos.ListarObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasos;
import com.ensambla.backend.domain.useCase.objetivo.reemplazarMiniObjetivo.ReemplazarMiniObjetivo;
import com.ensambla.backend.infrastructure.ia.ObjetivosIaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
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
