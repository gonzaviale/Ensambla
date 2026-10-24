package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.receta.RecetaRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarReceta;
import com.ensambla.backend.domain.useCase.receta.obtenerReceta.ObtenerReceta;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RecetaUseCaseConfig {

    @Bean
    public ObtenerReceta obtenerReceta(NegocioService negocioService,
                                       ControlPlanService controlPlanService,
                                       Clock clock,
                                       RegistroDatosService registroDatosService,
                                       RecetaService recetaService) {
        return new ObtenerReceta(
                negocioService, controlPlanService, clock, registroDatosService, recetaService);
    }

    @Bean
    public GuardarReceta guardarReceta(NegocioService negocioService,
                                       ControlPlanService controlPlanService,
                                       RegistroDatosService registroDatosService,
                                       RecetaService recetaService,
                                       Clock clock) {
        return new GuardarReceta(
                negocioService, controlPlanService, registroDatosService, recetaService, clock);
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
