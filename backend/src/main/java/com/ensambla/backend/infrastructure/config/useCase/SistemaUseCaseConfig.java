package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.sistema.SistemaItemMapper;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.sistema.SistemaService;
import com.ensambla.backend.domain.useCase.sistema.EliminarSistemaUseCase;
import com.ensambla.backend.domain.useCase.sistema.GuardarSistemaUseCase;
import com.ensambla.backend.domain.useCase.sistema.ListarSistemasUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SistemaUseCaseConfig {

    @Bean
    public SistemaItemMapper sistemaItemMapper() {
        return new SistemaItemMapper();
    }

    @Bean
    public ListarSistemasUseCase listarSistemasUseCase(
            NegocioService negocioService, SistemaService sistemaService, SistemaItemMapper sistemaItemMapper) {
        return new ListarSistemasUseCase(negocioService, sistemaService, sistemaItemMapper);
    }

    @Bean
    public GuardarSistemaUseCase guardarSistemaUseCase(
            NegocioService negocioService,
            SistemaService sistemaService,
            SistemaItemMapper sistemaItemMapper,
            Clock clock) {
        return new GuardarSistemaUseCase(negocioService, sistemaService, sistemaItemMapper, clock);
    }

    @Bean
    public EliminarSistemaUseCase eliminarSistemaUseCase(
            NegocioService negocioService, SistemaService sistemaService) {
        return new EliminarSistemaUseCase(negocioService, sistemaService);
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
