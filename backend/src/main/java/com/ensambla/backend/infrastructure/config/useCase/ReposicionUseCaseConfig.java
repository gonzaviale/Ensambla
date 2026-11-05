package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.GeneradorPedidoPdfPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.service.reposicion.ReposicionService;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedor;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ReposicionUseCaseConfig {

    @Bean
    public ConsultarReposicion consultarReposicion(NegocioRepository negocioRepository,
                                                   ReposicionService reposicionService) {
        return new ConsultarReposicion(negocioRepository, reposicionService);
    }

    @Bean
    public ArmarPedidoProveedor armarPedidoProveedor(NegocioRepository negocioRepository,
                                                     ReposicionService reposicionService,
                                                     GeneradorPedidoPdfPort generadorPedidoPdfPort,
                                                     Clock clock) {
        return new ArmarPedidoProveedor(negocioRepository, reposicionService, generadorPedidoPdfPort, clock);
    }

    @Bean
    public ReposicionService reposicionService(ProyectoRepository proyectoRepository,
                                               DatosDeProyectoService datosDeProyectoService) {
        return new ReposicionService(proyectoRepository, datosDeProyectoService);
    }
}
