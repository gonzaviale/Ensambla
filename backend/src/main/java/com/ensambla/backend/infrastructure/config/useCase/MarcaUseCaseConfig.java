package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.LectorDePixelesPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.marca.guardarKitDeMarca.GuardarKitDeMarca;
import com.ensambla.backend.domain.useCase.marca.obtenerKitDeMarca.ObtenerKitDeMarca;
import com.ensambla.backend.domain.useCase.marca.obtenerLogoDeMarca.ObtenerLogoDeMarca;
import com.ensambla.backend.domain.useCase.marca.subirLogoDeMarca.SubirLogoDeMarca;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class MarcaUseCaseConfig {

    @Bean
    public ObtenerKitDeMarca obtenerKitDeMarca(
            NegocioRepository negocioRepository,
            KitDeMarcaRepository kitDeMarcaRepository,
            Clock clock) {
        return new ObtenerKitDeMarca(negocioRepository, kitDeMarcaRepository, clock);
    }

    @Bean
    public GuardarKitDeMarca guardarKitDeMarca(
            NegocioRepository negocioRepository,
            KitDeMarcaRepository kitDeMarcaRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new GuardarKitDeMarca(negocioRepository, kitDeMarcaRepository, transaccionPort, clock);
    }

    @Bean
    public SubirLogoDeMarca subirLogoDeMarca(
            NegocioRepository negocioRepository,
            KitDeMarcaRepository kitDeMarcaRepository,
            LectorDePixelesPort lectorDePixelesPort,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new SubirLogoDeMarca(
                negocioRepository, kitDeMarcaRepository, lectorDePixelesPort, transaccionPort, clock);
    }

    @Bean
    public ObtenerLogoDeMarca obtenerLogoDeMarca(
            NegocioRepository negocioRepository,
            KitDeMarcaRepository kitDeMarcaRepository) {
        return new ObtenerLogoDeMarca(negocioRepository, kitDeMarcaRepository);
    }
}
