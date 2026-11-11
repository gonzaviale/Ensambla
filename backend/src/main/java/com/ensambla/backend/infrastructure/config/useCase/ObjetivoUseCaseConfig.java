package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.objetivo.crearObjetivo.CrearObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.eliminarObjetivo.EliminarObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.listarObjetivos.ListarObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasos;
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
    public CrearObjetivo crearObjetivo(
            NegocioRepository negocioRepository,
            ObjetivoRepository objetivoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new CrearObjetivo(
                negocioRepository,
                objetivoRepository,
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
