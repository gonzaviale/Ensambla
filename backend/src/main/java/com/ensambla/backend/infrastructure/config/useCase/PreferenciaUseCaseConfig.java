package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
import com.ensambla.backend.domain.service.preferencia.PreferenciaService;
import com.ensambla.backend.domain.useCase.preferencia.guardarPreferencias.GuardarPreferencias;
import com.ensambla.backend.domain.useCase.preferencia.obtenerPreferencias.ObtenerPreferencias;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PreferenciaUseCaseConfig {

    @Bean
    public ObtenerPreferencias obtenerPreferencias(
            PreferenciaService preferenciaService,
            Clock clock) {
        return new ObtenerPreferencias(preferenciaService, clock);
    }

    @Bean
    public GuardarPreferencias guardarPreferencias(
            PreferenciaService preferenciaService,
            Clock clock) {
        return new GuardarPreferencias(preferenciaService, clock);
    }

    @Bean
    public PreferenciaService preferenciaService(
            PreferenciaRepository preferenciaRepository,
            TransaccionPort transaccionPort) {
        return new PreferenciaService(preferenciaRepository, transaccionPort);
    }
}
