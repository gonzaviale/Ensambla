package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.sistema.eliminarSistema.EliminarSistema;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistema;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SistemaUseCaseConfig {

    @Bean
    public ListarSistemas listarSistemas(NegocioService negocioService, SistemaRepository sistemaRepository) {
        return new ListarSistemas(negocioService, sistemaRepository);
    }

    @Bean
    public GuardarSistema guardarSistema(
            NegocioService negocioService,
            SistemaRepository sistemaRepository,
            UsoPlanRepository usoPlanRepository,
            ControlPlanService controlPlanService,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new GuardarSistema(
                negocioService, sistemaRepository, usoPlanRepository, controlPlanService, transaccionPort, clock);
    }

    @Bean
    public EliminarSistema eliminarSistema(
            NegocioService negocioService, ProyectoRepository proyectoRepository, TransaccionPort transaccionPort) {
        return new EliminarSistema(negocioService, proyectoRepository, transaccionPort);
    }
}
