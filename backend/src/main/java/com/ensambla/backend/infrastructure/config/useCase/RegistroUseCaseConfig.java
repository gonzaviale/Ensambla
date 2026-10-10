package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.registro.CrearRegistroMapper;
import com.ensambla.backend.domain.mapper.registro.EditarRegistroMapper;
import com.ensambla.backend.domain.mapper.registro.ListarRegistrosMapper;
import com.ensambla.backend.domain.mapper.registro.ObtenerRegistroMapper;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.registro.CrearRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.EditarRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.EliminarRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.ListarRegistrosUseCase;
import com.ensambla.backend.domain.useCase.registro.ObtenerRegistroUseCase;
import org.springframework.beans.factory.ObjectProvider;
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
                                                     RegistroDatosService registroDatosService,
                                                     CrearRegistroMapper crearRegistroMapper,
                                                     ObjectProvider<Clock> clock) {
        return new CrearRegistroUseCase(negocioService, bloqueService, campoService, registroDatosService,
                crearRegistroMapper, clock.getIfAvailable(Clock::systemUTC));
    }

    @Bean
    public ListarRegistrosUseCase listarRegistrosUseCase(NegocioService negocioService,
                                                         BloqueService bloqueService,
                                                         CampoService campoService,
                                                         RegistroDatosService registroDatosService,
                                                         ListarRegistrosMapper listarRegistrosMapper) {
        return new ListarRegistrosUseCase(
                negocioService, bloqueService, campoService, registroDatosService, listarRegistrosMapper);
    }

    @Bean
    public ObtenerRegistroUseCase obtenerRegistroUseCase(NegocioService negocioService,
                                                         RegistroDatosService registroDatosService,
                                                         ObtenerRegistroMapper obtenerRegistroMapper) {
        return new ObtenerRegistroUseCase(negocioService, registroDatosService, obtenerRegistroMapper);
    }

    @Bean
    public EditarRegistroUseCase editarRegistroUseCase(NegocioService negocioService,
                                                       CampoService campoService,
                                                       RegistroDatosService registroDatosService,
                                                       EditarRegistroMapper editarRegistroMapper,
                                                       ObjectProvider<Clock> clock) {
        return new EditarRegistroUseCase(negocioService, campoService, registroDatosService, editarRegistroMapper,
                clock.getIfAvailable(Clock::systemUTC));
    }

    @Bean
    public EliminarRegistroUseCase eliminarRegistroUseCase(NegocioService negocioService,
                                                           RegistroDatosService registroDatosService) {
        return new EliminarRegistroUseCase(negocioService, registroDatosService);
    }
}
