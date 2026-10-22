package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.preferencia.PreferenciasItemMapper;
import com.ensambla.backend.domain.service.preferencia.PreferenciaService;
import com.ensambla.backend.domain.useCase.preferencia.GuardarPreferenciasUseCase;
import com.ensambla.backend.domain.useCase.preferencia.ObtenerPreferenciasUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PreferenciaUseCaseConfig {

    @Bean
    public PreferenciasItemMapper preferenciasItemMapper() {
        return new PreferenciasItemMapper();
    }

    @Bean
    public ObtenerPreferenciasUseCase obtenerPreferenciasUseCase(
            PreferenciaService preferenciaService,
            PreferenciasItemMapper preferenciasItemMapper,
            Clock clock) {
        return new ObtenerPreferenciasUseCase(preferenciaService, preferenciasItemMapper, clock);
    }

    @Bean
    public GuardarPreferenciasUseCase guardarPreferenciasUseCase(
            PreferenciaService preferenciaService,
            PreferenciasItemMapper preferenciasItemMapper,
            Clock clock) {
        return new GuardarPreferenciasUseCase(preferenciaService, preferenciasItemMapper, clock);
    }
}
