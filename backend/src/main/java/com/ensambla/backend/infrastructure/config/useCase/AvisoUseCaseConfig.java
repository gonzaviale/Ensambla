package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.aviso.AvisoItemMapper;
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
import com.ensambla.backend.domain.useCase.aviso.AplicarAvisoUseCase;
import com.ensambla.backend.domain.useCase.aviso.ContarAvisosUseCase;
import com.ensambla.backend.domain.useCase.aviso.DescartarAvisoUseCase;
import com.ensambla.backend.domain.useCase.aviso.EjecutarMotorAvisosUseCase;
import com.ensambla.backend.domain.useCase.aviso.ListarAvisosUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

@Configuration
public class AvisoUseCaseConfig {

    @Bean
    public AvisoItemMapper avisoItemMapper() {
        return new AvisoItemMapper();
    }

    @Bean
    public EjecutarMotorAvisosUseCase ejecutarMotorAvisosUseCase(
            NegocioService negocioService,
            MotorAvisosService motorAvisosService,
            Clock clock) {
        return new EjecutarMotorAvisosUseCase(negocioService, motorAvisosService, clock);
    }

    @Bean
    public ListarAvisosUseCase listarAvisosUseCase(
            NegocioService negocioService,
            AvisoService avisoService,
            AvisoItemMapper avisoItemMapper, AccesoContenidoService accesoContenidoService, Clock clock) {
        return new ListarAvisosUseCase(negocioService, avisoService, avisoItemMapper, accesoContenidoService, clock);
    }

    @Bean
    public ContarAvisosUseCase contarAvisosUseCase(NegocioService negocioService, AvisoService avisoService) {
        return new ContarAvisosUseCase(negocioService, avisoService);
    }

    @Bean
    public AplicarAvisoUseCase aplicarAvisoUseCase(
            NegocioService negocioService,
            AvisoService avisoService,
            AvisoItemMapper avisoItemMapper,
            Clock clock, AccesoContenidoService accesoContenidoService) {
        return new AplicarAvisoUseCase(negocioService, avisoService, avisoItemMapper, clock, accesoContenidoService);
    }

    @Bean
    public DescartarAvisoUseCase descartarAvisoUseCase(
            NegocioService negocioService,
            AvisoService avisoService,
            AvisoItemMapper avisoItemMapper,
            Clock clock, AccesoContenidoService accesoContenidoService) {
        return new DescartarAvisoUseCase(negocioService, avisoService, avisoItemMapper, clock, accesoContenidoService);
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
