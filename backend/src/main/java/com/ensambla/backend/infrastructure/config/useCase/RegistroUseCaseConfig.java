package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
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
            NegocioService negocioService,
            BloqueService bloqueService,
            CampoService campoService,
            RegistroDatosService registroDatosService,
            ControlPlanService controlPlanService,
            RegistroRepository registroRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new CrearRegistro(
                negocioService, bloqueService, campoService, registroDatosService, controlPlanService,
                registroRepository, transaccionPort, clock);
    }

    @Bean
    public ListarRegistros listarRegistros(
            NegocioService negocioService,
            BloqueService bloqueService,
            CampoService campoService,
            RegistroDatosService registroDatosService,
            RegistroRepository registroRepository) {
        return new ListarRegistros(
                negocioService, bloqueService, campoService, registroDatosService, registroRepository);
    }

    @Bean
    public ListarReferencias listarReferencias(NegocioService negocioService,
                                               BloqueService bloqueService,
                                               CampoService campoService,
                                               RegistroDatosService registroDatosService) {
        return new ListarReferencias(
                negocioService, bloqueService, campoService, registroDatosService);
    }

    @Bean
    public ObtenerRegistro obtenerRegistro(NegocioService negocioService,
                                           CampoService campoService,
                                           RegistroDatosService registroDatosService) {
        return new ObtenerRegistro(negocioService, campoService, registroDatosService);
    }

    @Bean
    public EditarRegistro editarRegistro(NegocioService negocioService,
                                         CampoService campoService,
                                         RegistroDatosService registroDatosService,
                                         Clock clock) {
        return new EditarRegistro(negocioService, campoService, registroDatosService,
                clock);
    }

    @Bean
    public ObtenerFotoOrigen obtenerFotoOrigen(
            NegocioService negocioService,
            RegistroDatosService registroDatosService,
            LoteSubidaService loteSubidaService,
            LoteSubidaRepository loteSubidaRepository) {
        return new ObtenerFotoOrigen(negocioService, registroDatosService, loteSubidaService, loteSubidaRepository);
    }

    @Bean
    public EliminarRegistro eliminarRegistro(
            NegocioService negocioService,
            RegistroDatosService registroDatosService,
            RegistroRepository registroRepository,
            TransaccionPort transaccionPort) {
        return new EliminarRegistro(negocioService, registroDatosService, registroRepository, transaccionPort);
    }

    @Bean
    public RegistroDatosService registroDatosService(
            RegistroRepository registroRepository,
            ControlPlanService controlPlanService,
            TransaccionPort transaccionPort) {
        return new RegistroDatosService(registroRepository, controlPlanService, transaccionPort);
    }
}
