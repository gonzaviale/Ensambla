package com.ensambla.backend.infrastructure.config.useCase;


import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.receta.RecetaRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarReceta;
import com.ensambla.backend.domain.useCase.receta.obtenerReceta.ObtenerReceta;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RecetaUseCaseConfig {

    @Bean
    public ObtenerReceta obtenerReceta(
            NegocioRepository negocioRepository,
            ControlPlanService controlPlanService,
            Clock clock,
            RegistroRepository registroRepository,
            RecetaService recetaService,
            RecetaRepository recetaRepository) {
        return new ObtenerReceta(
                negocioRepository, controlPlanService, clock, registroRepository, recetaService, recetaRepository);
    }

    @Bean
    public GuardarReceta guardarReceta(
            NegocioRepository negocioRepository,
            ControlPlanService controlPlanService,
            RecetaService recetaService,
            RecetaRepository recetaRepository,
            CampoRepository campoRepository,
            RegistroRepository registroRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new GuardarReceta(
                negocioRepository, controlPlanService, recetaService, recetaRepository,
                campoRepository, registroRepository, transaccionPort, clock);
    }

    @Bean
    public RecetaService recetaService(CampoRepository campoRepository, RegistroRepository registroRepository) {
        return new RecetaService(campoRepository, registroRepository);
    }
}
