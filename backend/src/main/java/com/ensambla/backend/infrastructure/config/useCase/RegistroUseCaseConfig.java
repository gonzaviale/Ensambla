package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.registro.crearRegistro.CrearRegistro;
import com.ensambla.backend.domain.useCase.registro.editarRegistro.EditarRegistro;
import com.ensambla.backend.domain.useCase.registro.eliminarRegistro.EliminarRegistro;
import com.ensambla.backend.domain.useCase.registro.listarReferencias.ListarReferencias;
import com.ensambla.backend.domain.useCase.registro.listarRegistros.ListarRegistros;
import com.ensambla.backend.domain.useCase.registro.obtenerFotoOrigen.ObtenerFotoOrigen;
import com.ensambla.backend.domain.useCase.registro.obtenerRegistro.ObtenerRegistro;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RegistroUseCaseConfig {

    @Bean
    public CrearRegistro crearRegistro(
            NegocioRepository negocioRepository,
            BloqueRepository bloqueRepository,
            CampoRepository campoRepository,
            RegistroDatosService registroDatosService,
            ControlPlanService controlPlanService,
            RegistroRepository registroRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new CrearRegistro(
                negocioRepository, bloqueRepository, campoRepository, registroDatosService, controlPlanService,
                registroRepository, transaccionPort, clock);
    }

    @Bean
    public ListarRegistros listarRegistros(
            NegocioRepository negocioRepository,
            BloqueRepository bloqueRepository,
            CampoRepository campoRepository,
            RegistroRepository registroRepository) {
        return new ListarRegistros(
                negocioRepository, bloqueRepository, campoRepository, registroRepository);
    }

    @Bean
    public ListarReferencias listarReferencias(NegocioRepository negocioRepository,
                                               BloqueRepository bloqueRepository,
                                               CampoRepository campoRepository,
                                               RegistroDatosService registroDatosService) {
        return new ListarReferencias(
                negocioRepository, bloqueRepository, campoRepository, registroDatosService);
    }

    @Bean
    public ObtenerRegistro obtenerRegistro(NegocioRepository negocioRepository,
                                           CampoRepository campoRepository,
                                           RegistroRepository registroRepository) {
        return new ObtenerRegistro(negocioRepository, campoRepository, registroRepository);
    }

    @Bean
    public EditarRegistro editarRegistro(NegocioRepository negocioRepository,
                                         CampoRepository campoRepository,
                                         RegistroDatosService registroDatosService,
            RegistroRepository registroRepository,
                                         Clock clock) {
        return new EditarRegistro(negocioRepository, campoRepository, registroDatosService, registroRepository,
                clock);
    }

    @Bean
    public ObtenerFotoOrigen obtenerFotoOrigen(
            NegocioRepository negocioRepository,
            RegistroRepository registroRepository,
            ArchivoCuadernoPort archivoCuadernoPort,
            LoteSubidaRepository loteSubidaRepository) {
        return new ObtenerFotoOrigen(negocioRepository, registroRepository, archivoCuadernoPort, loteSubidaRepository);
    }

    @Bean
    public EliminarRegistro eliminarRegistro(
            NegocioRepository negocioRepository,
            RegistroRepository registroRepository,
            TransaccionPort transaccionPort) {
        return new EliminarRegistro(negocioRepository, registroRepository, transaccionPort);
    }

    @Bean
    public RegistroDatosService registroDatosService(
            RegistroRepository registroRepository,
            TransaccionPort transaccionPort) {
        return new RegistroDatosService(registroRepository, transaccionPort);
    }
}
