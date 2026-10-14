package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.lote.FotoArchivoMapper;
import com.ensambla.backend.domain.mapper.lote.LoteItemMapper;
import com.ensambla.backend.domain.mapper.lote.ObtenerLoteMapper;
import com.ensambla.backend.domain.mapper.lote.SubirLoteMapper;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.lote.ObtenerFotoLoteUseCase;
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
    public FotoArchivoMapper fotoArchivoMapper() {
        return new FotoArchivoMapper();
    }

    @Bean
    public ObtenerFotoLoteUseCase obtenerFotoLoteUseCase(
            NegocioService negocioService,
            LoteSubidaService loteSubidaService,
            FotoArchivoMapper fotoArchivoMapper) {
        return new ObtenerFotoLoteUseCase(
                negocioService,
                loteSubidaService,
                fotoArchivoMapper);
    }

    @Bean
    public SubirLoteUseCase subirLoteUseCase(
            NegocioService negocioService,
            LoteSubidaService loteSubidaService,
            BloqueService bloqueService,
            SubirLoteMapper subirLoteMapper,
            Clock clock) {
        return new SubirLoteUseCase(
                negocioService,
                loteSubidaService,
                bloqueService,
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
