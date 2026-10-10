package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.registro.CrearRegistroMapper;
import com.ensambla.backend.domain.mapper.registro.EditarRegistroMapper;
import com.ensambla.backend.domain.mapper.registro.ListarRegistrosMapper;
import com.ensambla.backend.domain.mapper.registro.ObtenerRegistroMapper;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroService;
import com.ensambla.backend.domain.useCase.registro.CrearRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.EditarRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.EliminarRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.ListarRegistrosUseCase;
import com.ensambla.backend.domain.useCase.registro.ObtenerRegistroUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RegistroUseCaseConfig {

    @Bean
    public CrearRegistroMapper crearRegistroMapper() {
        return new CrearRegistroMapper();
    }

    @Bean
    public ListarRegistrosMapper listarRegistrosMapper() {
        return new ListarRegistrosMapper();
    }

    @Bean
    public ObtenerRegistroMapper obtenerRegistroMapper() {
        return new ObtenerRegistroMapper();
    }

    @Bean
    public EditarRegistroMapper editarRegistroMapper() {
        return new EditarRegistroMapper();
    }

    @Bean
    public CrearRegistroUseCase crearRegistroUseCase(NegocioService negocioService,
                                                     BloqueService bloqueService,
                                                     CampoService campoService,
                                                     RegistroService registroService,
                                                     CrearRegistroMapper crearRegistroMapper,
                                                     Clock clock) {
        return new CrearRegistroUseCase(
                negocioService, bloqueService, campoService, registroService, crearRegistroMapper, clock);
    }

    @Bean
    public ListarRegistrosUseCase listarRegistrosUseCase(NegocioService negocioService,
                                                         BloqueService bloqueService,
                                                         CampoService campoService,
                                                         RegistroService registroService,
                                                         ListarRegistrosMapper listarRegistrosMapper) {
        return new ListarRegistrosUseCase(
                negocioService, bloqueService, campoService, registroService, listarRegistrosMapper);
    }

    @Bean
    public ObtenerRegistroUseCase obtenerRegistroUseCase(NegocioService negocioService,
                                                         RegistroService registroService,
                                                         ObtenerRegistroMapper obtenerRegistroMapper) {
        return new ObtenerRegistroUseCase(negocioService, registroService, obtenerRegistroMapper);
    }

    @Bean
    public EditarRegistroUseCase editarRegistroUseCase(NegocioService negocioService,
                                                       CampoService campoService,
                                                       RegistroService registroService,
                                                       EditarRegistroMapper editarRegistroMapper,
                                                       Clock clock) {
        return new EditarRegistroUseCase(negocioService, campoService, registroService, editarRegistroMapper, clock);
    }

    @Bean
    public EliminarRegistroUseCase eliminarRegistroUseCase(NegocioService negocioService,
                                                           RegistroService registroService) {
        return new EliminarRegistroUseCase(negocioService, registroService);
    }
}
