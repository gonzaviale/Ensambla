package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.aviso.ReglaAviso;
import com.ensambla.backend.domain.model.aviso.ReglaStockBajo;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.aviso.AccionAvisoService;
import com.ensambla.backend.domain.service.aviso.AvisoService;
import com.ensambla.backend.domain.service.aviso.ContextoNegocioService;
import com.ensambla.backend.domain.service.aviso.MotorAvisosService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.aviso.aplicarAviso.AplicarAviso;
import com.ensambla.backend.domain.useCase.aviso.contarAvisos.ContarAvisos;
import com.ensambla.backend.domain.useCase.aviso.descartarAviso.DescartarAviso;
import com.ensambla.backend.domain.useCase.aviso.ejecutarMotorAvisos.EjecutarMotorAvisos;
import com.ensambla.backend.domain.useCase.aviso.listarAvisos.ListarAvisos;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

@Configuration
public class AvisoUseCaseConfig {

    @Bean
    public EjecutarMotorAvisos ejecutarMotorAvisos(
            NegocioService negocioService,
            MotorAvisosService motorAvisosService,
            Clock clock) {
        return new EjecutarMotorAvisos(negocioService, motorAvisosService, clock);
    }

    @Bean
    public ListarAvisos listarAvisos(
            NegocioService negocioService,
            AvisoService avisoService,
            AccesoContenidoService accesoContenidoService, Clock clock) {
        return new ListarAvisos(negocioService, avisoService, accesoContenidoService, clock);
    }

    @Bean
    public ContarAvisos contarAvisos(NegocioService negocioService, AvisoService avisoService) {
        return new ContarAvisos(negocioService, avisoService);
    }

    @Bean
    public AplicarAviso aplicarAviso(
            NegocioService negocioService,
            AvisoService avisoService,
            Clock clock, AccesoContenidoService accesoContenidoService) {
        return new AplicarAviso(negocioService, avisoService, clock, accesoContenidoService);
    }

    @Bean
    public DescartarAviso descartarAviso(
            NegocioService negocioService,
            AvisoService avisoService,
            Clock clock, AccesoContenidoService accesoContenidoService) {
        return new DescartarAviso(negocioService, avisoService, clock, accesoContenidoService);
    }

    @Bean
    public AccionAvisoService accionAvisoService(
            RegistroDatosService registroDatosService,
            CampoService campoService,
            TransaccionPort transaccionPort) {
        return new AccionAvisoService(registroDatosService, campoService, transaccionPort);
    }

    @Bean
    public AvisoService avisoService(
            AvisoRepository avisoRepository,
            AccionAvisoService accionAvisoService,
            TransaccionPort transaccionPort) {
        return new AvisoService(avisoRepository, accionAvisoService, transaccionPort);
    }

    @Bean
    public ContextoNegocioService contextoNegocioService(
            ProyectoRepository proyectoRepository,
            RegistroRepository registroRepository) {
        return new ContextoNegocioService(proyectoRepository, registroRepository);
    }

    @Bean
    public ReglaStockBajo reglaStockBajo() {
        return new ReglaStockBajo();
    }

    @Bean
    public MotorAvisosService motorAvisosService(
            List<ReglaAviso> reglas,
            ContextoNegocioService contextoNegocioService,
            AvisoRepository avisoRepository,
            TransaccionPort transaccionPort) {
        return new MotorAvisosService(reglas, contextoNegocioService, avisoRepository, transaccionPort);
    }
}
