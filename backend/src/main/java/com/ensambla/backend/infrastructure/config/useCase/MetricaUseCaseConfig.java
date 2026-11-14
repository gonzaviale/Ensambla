package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.metrica.CatalogoMetricas;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.metrica.MetricaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.metrica.crearMetrica.CrearMetrica;
import com.ensambla.backend.domain.useCase.metrica.eliminarMetrica.EliminarMetrica;
import com.ensambla.backend.domain.useCase.metrica.listarMetricas.ListarMetricas;
import com.ensambla.backend.domain.useCase.metrica.listarSugeridas.ListarSugeridas;
import com.ensambla.backend.domain.useCase.metrica.previsualizarMetrica.PrevisualizarMetrica;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class MetricaUseCaseConfig {

    @Bean
    public ListarMetricas listarMetricas(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository,
            MetricaRepository metricaRepository,
            DatosDeProyectoService datosDeProyectoService,
            MotorMetricas motorMetricas,
            Clock clock) {
        return new ListarMetricas(
                negocioRepository,
                proyectoRepository,
                metricaRepository,
                datosDeProyectoService,
                motorMetricas,
                clock);
    }

    @Bean
    public ListarSugeridas listarSugeridas(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository,
            MetricaRepository metricaRepository,
            CatalogoMetricas catalogoMetricas,
            DatosDeProyectoService datosDeProyectoService,
            MotorMetricas motorMetricas,
            Clock clock) {
        return new ListarSugeridas(
                negocioRepository,
                proyectoRepository,
                metricaRepository,
                catalogoMetricas,
                datosDeProyectoService,
                motorMetricas,
                clock);
    }

    @Bean
    public PrevisualizarMetrica previsualizarMetrica(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository,
            DatosDeProyectoService datosDeProyectoService,
            MotorMetricas motorMetricas,
            Clock clock) {
        return new PrevisualizarMetrica(
                negocioRepository,
                proyectoRepository,
                datosDeProyectoService,
                motorMetricas,
                clock);
    }

    @Bean
    public CrearMetrica crearMetrica(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository,
            MetricaRepository metricaRepository,
            DatosDeProyectoService datosDeProyectoService,
            MotorMetricas motorMetricas,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new CrearMetrica(
                negocioRepository,
                proyectoRepository,
                metricaRepository,
                datosDeProyectoService,
                motorMetricas,
                transaccionPort,
                clock);
    }

    @Bean
    public EliminarMetrica eliminarMetrica(
            NegocioRepository negocioRepository,
            MetricaRepository metricaRepository,
            TransaccionPort transaccionPort) {
        return new EliminarMetrica(negocioRepository, metricaRepository, transaccionPort);
    }
}
