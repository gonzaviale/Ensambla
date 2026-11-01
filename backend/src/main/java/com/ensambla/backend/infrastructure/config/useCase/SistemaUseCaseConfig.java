package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
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
    public ListarSistemas listarSistemas(NegocioRepository negocioRepository, SistemaRepository sistemaRepository) {
        return new ListarSistemas(negocioRepository, sistemaRepository);
    }

    @Bean
    public GuardarSistema guardarSistema(
            NegocioRepository negocioRepository,
            SistemaRepository sistemaRepository,
            UsoPlanRepository usoPlanRepository,
            ControlPlanService controlPlanService,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new GuardarSistema(
                negocioRepository, sistemaRepository, usoPlanRepository, controlPlanService, transaccionPort, clock);
    }

    @Bean
    public EliminarSistema eliminarSistema(
            NegocioRepository negocioRepository, ProyectoRepository proyectoRepository,
                    TransaccionPort transaccionPort) {
        return new EliminarSistema(negocioRepository, proyectoRepository, transaccionPort);
    }
}
