package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.aviso.AvisoItemMapper;
import com.ensambla.backend.domain.service.aviso.AvisoService;
import com.ensambla.backend.domain.service.aviso.MotorAvisosService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.aviso.AplicarAvisoUseCase;
import com.ensambla.backend.domain.useCase.aviso.ContarAvisosUseCase;
import com.ensambla.backend.domain.useCase.aviso.DescartarAvisoUseCase;
import com.ensambla.backend.domain.useCase.aviso.EjecutarMotorAvisosUseCase;
import com.ensambla.backend.domain.useCase.aviso.ListarAvisosUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AvisoUseCaseConfig {

    @Bean
    public AvisoItemMapper avisoItemMapper() {
        return new AvisoItemMapper();
    }

    @Bean
    public EjecutarMotorAvisosUseCase ejecutarMotorAvisosUseCase(
            NegocioService negocioService,
            MotorAvisosService motorAvisosService,
            Clock clock) {
        return new EjecutarMotorAvisosUseCase(negocioService, motorAvisosService, clock);
    }

    @Bean
    public ListarAvisosUseCase listarAvisosUseCase(
            NegocioService negocioService,
            AvisoService avisoService,
            AvisoItemMapper avisoItemMapper) {
        return new ListarAvisosUseCase(negocioService, avisoService, avisoItemMapper);
    }

    @Bean
    public ContarAvisosUseCase contarAvisosUseCase(NegocioService negocioService, AvisoService avisoService) {
        return new ContarAvisosUseCase(negocioService, avisoService);
    }

    @Bean
    public AplicarAvisoUseCase aplicarAvisoUseCase(
            NegocioService negocioService,
            AvisoService avisoService,
            AvisoItemMapper avisoItemMapper,
            Clock clock) {
        return new AplicarAvisoUseCase(negocioService, avisoService, avisoItemMapper, clock);
    }

    @Bean
    public DescartarAvisoUseCase descartarAvisoUseCase(
            NegocioService negocioService,
            AvisoService avisoService,
            AvisoItemMapper avisoItemMapper,
            Clock clock) {
        return new DescartarAvisoUseCase(negocioService, avisoService, avisoItemMapper, clock);
    }
}
