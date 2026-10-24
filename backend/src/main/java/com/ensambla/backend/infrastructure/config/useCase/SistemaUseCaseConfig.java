package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.sistema.SistemaService;
import com.ensambla.backend.domain.useCase.sistema.eliminarSistema.EliminarSistema;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistema;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SistemaUseCaseConfig {

    @Bean
    public ListarSistemas listarSistemas(
            NegocioService negocioService, SistemaService sistemaService) {
        return new ListarSistemas(negocioService, sistemaService);
    }

    @Bean
    public GuardarSistema guardarSistema(
            NegocioService negocioService,
            SistemaService sistemaService,
            Clock clock) {
        return new GuardarSistema(negocioService, sistemaService, clock);
    }

    @Bean
    public EliminarSistema eliminarSistema(
            NegocioService negocioService, SistemaService sistemaService) {
        return new EliminarSistema(negocioService, sistemaService);
    }

    @Bean
    public SistemaService sistemaService(
            SistemaRepository sistemaRepository,
            ProyectoRepository proyectoRepository,
            ControlPlanService controlPlanService,
            UsoPlanRepository usoPlanRepository,
            TransaccionPort transaccionPort) {
        return new SistemaService(
                sistemaRepository, proyectoRepository, controlPlanService, usoPlanRepository, transaccionPort);
    }
}
