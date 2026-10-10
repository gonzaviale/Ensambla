package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.campo.CampoItemMapper;
import com.ensambla.backend.domain.mapper.campo.CrearCampoMapper;
import com.ensambla.backend.domain.mapper.campo.EditarCampoMapper;
import com.ensambla.backend.domain.mapper.campo.ListarCamposMapper;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.campo.CrearCampoUseCase;
import com.ensambla.backend.domain.useCase.campo.EditarCampoUseCase;
import com.ensambla.backend.domain.useCase.campo.EliminarCampoUseCase;
import com.ensambla.backend.domain.useCase.campo.ListarCamposUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CampoUseCaseConfig {

    @Bean
    public CampoItemMapper campoItemMapper() {
        return new CampoItemMapper();
    }

    @Bean
    public ListarCamposMapper listarCamposMapper(CampoItemMapper campoItemMapper) {
        return new ListarCamposMapper(campoItemMapper);
    }

    @Bean
    public ListarCamposUseCase listarCamposUseCase(
            NegocioService negocioService,
            BloqueService bloqueService,
            CampoService campoService,
            ListarCamposMapper listarCamposMapper) {
        return new ListarCamposUseCase(
                negocioService,
                bloqueService,
                campoService,
                listarCamposMapper);
    }

    @Bean
    public CrearCampoMapper crearCampoMapper(CampoItemMapper campoItemMapper) {
        return new CrearCampoMapper(campoItemMapper);
    }

    @Bean
    public CrearCampoUseCase crearCampoUseCase(
            NegocioService negocioService,
            BloqueService bloqueService,
            CampoService campoService,
            CrearCampoMapper crearCampoMapper,
            Clock clock) {
        return new CrearCampoUseCase(
                negocioService,
                bloqueService,
                campoService,
                crearCampoMapper,
                clock);
    }

    @Bean
    public EditarCampoMapper editarCampoMapper(CampoItemMapper campoItemMapper) {
        return new EditarCampoMapper(campoItemMapper);
    }

    @Bean
    public EditarCampoUseCase editarCampoUseCase(
            NegocioService negocioService,
            CampoService campoService,
            BloqueService bloqueService,
            EditarCampoMapper editarCampoMapper,
            Clock clock) {
        return new EditarCampoUseCase(
                negocioService,
                campoService,
                bloqueService,
                editarCampoMapper,
                clock);
    }

    @Bean
    public EliminarCampoUseCase eliminarCampoUseCase(
            NegocioService negocioService,
            CampoService campoService) {
        return new EliminarCampoUseCase(
                negocioService,
                campoService);
    }
}
