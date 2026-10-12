package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.lote.FotoArchivoMapper;
import com.ensambla.backend.domain.mapper.registro.CrearRegistroMapper;
import com.ensambla.backend.domain.mapper.registro.EditarRegistroMapper;
import com.ensambla.backend.domain.mapper.registro.ListarReferenciasMapper;
import com.ensambla.backend.domain.mapper.registro.ListarRegistrosMapper;
import com.ensambla.backend.domain.mapper.registro.ObtenerRegistroMapper;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.registro.CrearRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.EditarRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.EliminarRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.ListarReferenciasUseCase;
import com.ensambla.backend.domain.useCase.registro.ListarRegistrosUseCase;
import com.ensambla.backend.domain.useCase.registro.ObtenerFotoOrigenUseCase;
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
    public ListarReferenciasMapper listarReferenciasMapper() {
        return new ListarReferenciasMapper();
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
                                                     Clock clock) {
        return new CrearRegistroUseCase(negocioService, bloqueService, campoService, registroDatosService,
                crearRegistroMapper, clock);
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
    public ListarReferenciasUseCase listarReferenciasUseCase(NegocioService negocioService,
                                                             BloqueService bloqueService,
                                                             CampoService campoService,
                                                             RegistroDatosService registroDatosService,
                                                             ListarReferenciasMapper listarReferenciasMapper) {
        return new ListarReferenciasUseCase(
                negocioService, bloqueService, campoService, registroDatosService, listarReferenciasMapper);
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
                                                       Clock clock) {
        return new EditarRegistroUseCase(negocioService, campoService, registroDatosService, editarRegistroMapper,
                clock);
    }

    @Bean
    public ObtenerFotoOrigenUseCase obtenerFotoOrigenUseCase(NegocioService negocioService,
                                                             RegistroDatosService registroDatosService,
                                                             LoteSubidaService loteSubidaService,
                                                             FotoArchivoMapper fotoArchivoMapper) {
        return new ObtenerFotoOrigenUseCase(
                negocioService, registroDatosService, loteSubidaService, fotoArchivoMapper);
    }

    @Bean
    public EliminarRegistroUseCase eliminarRegistroUseCase(NegocioService negocioService,
                                                           RegistroDatosService registroDatosService) {
        return new EliminarRegistroUseCase(negocioService, registroDatosService);
    }
}
