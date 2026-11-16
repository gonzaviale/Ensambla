package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.DibujanteDeCartelPort;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel.ArmarOpcionesDeCartel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CartelUseCaseConfig {

    @Bean
    public ArmarOpcionesDeCartel armarOpcionesDeCartel(
            NegocioRepository negocioRepository,
            KitDeMarcaRepository kitDeMarcaRepository,
            DibujanteDeCartelPort dibujanteDeCartelPort,
            Clock clock) {
        return new ArmarOpcionesDeCartel(negocioRepository, kitDeMarcaRepository, dibujanteDeCartelPort, clock);
    }
}
