package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.lote.LoteItemMapper;
import com.ensambla.backend.domain.mapper.lote.ObtenerLoteMapper;
import com.ensambla.backend.domain.mapper.lote.SubirLoteMapper;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.lote.ObtenerLoteUseCase;
import com.ensambla.backend.domain.useCase.lote.SubirLoteUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class LoteUseCaseConfig {

    @Bean
    public LoteItemMapper loteItemMapper() {
        return new LoteItemMapper();
    }

    @Bean
    public SubirLoteMapper subirLoteMapper(LoteItemMapper loteItemMapper) {
        return new SubirLoteMapper(loteItemMapper);
    }

    @Bean
    public ObtenerLoteMapper obtenerLoteMapper(LoteItemMapper loteItemMapper) {
        return new ObtenerLoteMapper(loteItemMapper);
    }

    @Bean
    public SubirLoteUseCase subirLoteUseCase(
            NegocioService negocioService,
            LoteSubidaService loteSubidaService,
            SubirLoteMapper subirLoteMapper,
            Clock clock) {
        return new SubirLoteUseCase(
                negocioService,
                loteSubidaService,
                subirLoteMapper,
                clock);
    }

    @Bean
    public ObtenerLoteUseCase obtenerLoteUseCase(
            NegocioService negocioService,
            LoteSubidaService loteSubidaService,
            ObtenerLoteMapper obtenerLoteMapper) {
        return new ObtenerLoteUseCase(
                negocioService,
                loteSubidaService,
                obtenerLoteMapper);
    }
}
