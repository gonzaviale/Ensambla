package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.plan.IniciarPruebaMapper;
import com.ensambla.backend.domain.mapper.plan.ConsultarPlanesMapper;
import com.ensambla.backend.domain.service.plan.ConsultaPlanesService;
import com.ensambla.backend.domain.useCase.plan.ConsultarPlanesUseCase;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.PruebaPremiumService;
import com.ensambla.backend.domain.useCase.plan.IniciarPruebaUseCase;
import com.ensambla.backend.domain.useCase.plan.VencerPruebasUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PlanUseCaseConfig {

    @Bean
    public ConsultarPlanesMapper consultarPlanesMapper() {
        return new ConsultarPlanesMapper();
    }

    @Bean
    public ConsultarPlanesUseCase consultarPlanesUseCase(NegocioService negocioService,
                                                       ConsultaPlanesService consultaPlanesService,
                                                       ConsultarPlanesMapper consultarPlanesMapper,
                                                       Clock clock) {
        return new ConsultarPlanesUseCase(negocioService, consultaPlanesService, consultarPlanesMapper, clock);
    }

    @Bean
    public IniciarPruebaMapper iniciarPruebaMapper() {
        return new IniciarPruebaMapper();
    }

    @Bean
    public IniciarPruebaUseCase iniciarPruebaUseCase(NegocioService negocioService,
                                                   PruebaPremiumService pruebaPremiumService,
                                                   IniciarPruebaMapper iniciarPruebaMapper,
                                                   Clock clock) {
        return new IniciarPruebaUseCase(negocioService, pruebaPremiumService, iniciarPruebaMapper, clock);
    }

    @Bean
    public VencerPruebasUseCase vencerPruebasUseCase(PruebaPremiumService pruebaPremiumService, Clock clock) {
        return new VencerPruebasUseCase(pruebaPremiumService, clock);
    }
}
