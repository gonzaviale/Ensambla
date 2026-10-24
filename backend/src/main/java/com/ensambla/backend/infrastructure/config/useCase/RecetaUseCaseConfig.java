package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.receta.GuardarRecetaMapper;
import com.ensambla.backend.domain.mapper.receta.ObtenerRecetaMapper;
import com.ensambla.backend.domain.mapper.receta.RecetaItemMapper;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.receta.RecetaRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.receta.GuardarRecetaUseCase;
import com.ensambla.backend.domain.useCase.receta.ObtenerRecetaUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RecetaUseCaseConfig {

    @Bean
    public RecetaItemMapper recetaItemMapper() {
        return new RecetaItemMapper();
    }

    @Bean
    public ObtenerRecetaMapper obtenerRecetaMapper(RecetaItemMapper recetaItemMapper) {
        return new ObtenerRecetaMapper(recetaItemMapper);
    }

    @Bean
    public GuardarRecetaMapper guardarRecetaMapper(RecetaItemMapper recetaItemMapper) {
        return new GuardarRecetaMapper(recetaItemMapper);
    }

    @Bean
    public ObtenerRecetaUseCase obtenerRecetaUseCase(NegocioService negocioService,
                                                     ControlPlanService controlPlanService,
                                                     Clock clock,
                                                     RegistroDatosService registroDatosService,
                                                     RecetaService recetaService,
                                                     ObtenerRecetaMapper obtenerRecetaMapper) {
        return new ObtenerRecetaUseCase(
                negocioService, controlPlanService, clock, registroDatosService, recetaService, obtenerRecetaMapper);
    }

    @Bean
    public GuardarRecetaUseCase guardarRecetaUseCase(NegocioService negocioService,
                                                     ControlPlanService controlPlanService,
                                                     RegistroDatosService registroDatosService,
                                                     RecetaService recetaService,
                                                     GuardarRecetaMapper guardarRecetaMapper,
                                                     Clock clock) {
        return new GuardarRecetaUseCase(
                negocioService, controlPlanService, registroDatosService, recetaService, guardarRecetaMapper, clock);
    }

    @Bean
    public RecetaService recetaService(
            RecetaRepository recetaRepository,
            CampoRepository campoRepository,
            RegistroRepository registroRepository,
            TransaccionPort transaccionPort) {
        return new RecetaService(recetaRepository, campoRepository, registroRepository, transaccionPort);
    }
}
