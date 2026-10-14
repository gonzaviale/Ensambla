package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.receta.GuardarRecetaMapper;
import com.ensambla.backend.domain.mapper.receta.ObtenerRecetaMapper;
import com.ensambla.backend.domain.mapper.receta.RecetaItemMapper;
import com.ensambla.backend.domain.service.negocio.NegocioService;
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
                                                     RegistroDatosService registroDatosService,
                                                     RecetaService recetaService,
                                                     ObtenerRecetaMapper obtenerRecetaMapper) {
        return new ObtenerRecetaUseCase(negocioService, registroDatosService, recetaService, obtenerRecetaMapper);
    }

    @Bean
    public GuardarRecetaUseCase guardarRecetaUseCase(NegocioService negocioService,
                                                     RegistroDatosService registroDatosService,
                                                     RecetaService recetaService,
                                                     GuardarRecetaMapper guardarRecetaMapper,
                                                     Clock clock) {
        return new GuardarRecetaUseCase(
                negocioService, registroDatosService, recetaService, guardarRecetaMapper, clock);
    }
}
