package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
import com.ensambla.backend.domain.useCase.preferencia.guardarPreferencias.GuardarPreferencias;
import com.ensambla.backend.domain.useCase.preferencia.obtenerPreferencias.ObtenerPreferencias;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PreferenciaUseCaseConfig {

    @Bean
    public ObtenerPreferencias obtenerPreferencias(
            PreferenciaRepository preferenciaRepository,
            Clock clock) {
        return new ObtenerPreferencias(preferenciaRepository, clock);
    }

    @Bean
    public GuardarPreferencias guardarPreferencias(
            PreferenciaRepository preferenciaRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new GuardarPreferencias(preferenciaRepository, transaccionPort, clock);
    }

}
