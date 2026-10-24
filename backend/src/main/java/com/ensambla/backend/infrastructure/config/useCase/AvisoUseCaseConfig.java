package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.aviso.ReglaAviso;
import com.ensambla.backend.domain.model.aviso.ReglaStockBajo;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.aviso.AvisoService;
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
            NegocioRepository negocioRepository,
            List<ReglaAviso> reglas,
            ProyectoRepository proyectoRepository,
            RegistroRepository registroRepository,
            AvisoRepository avisoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new EjecutarMotorAvisos(negocioRepository, reglas, proyectoRepository, registroRepository,
                avisoRepository, transaccionPort, clock);
    }

    @Bean
    public ListarAvisos listarAvisos(
            NegocioService negocioService,
            AvisoRepository avisoRepository,
            AccesoContenidoService accesoContenidoService, Clock clock) {
        return new ListarAvisos(negocioService, avisoRepository, accesoContenidoService, clock);
    }

    @Bean
    public ContarAvisos contarAvisos(NegocioService negocioService, AvisoRepository avisoRepository) {
        return new ContarAvisos(negocioService, avisoRepository);
    }

    @Bean
    public AplicarAviso aplicarAviso(
            NegocioService negocioService,
            AvisoService avisoService,
            AccesoContenidoService accesoContenidoService,
            RegistroDatosService registroDatosService,
            CampoService campoService,
            AvisoRepository avisoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new AplicarAviso(negocioService, avisoService, accesoContenidoService, registroDatosService,
                campoService, avisoRepository, transaccionPort, clock);
    }

    @Bean
    public DescartarAviso descartarAviso(
            NegocioService negocioService,
            AvisoService avisoService,
            AccesoContenidoService accesoContenidoService,
            AvisoRepository avisoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new DescartarAviso(
                negocioService, avisoService, accesoContenidoService, avisoRepository, transaccionPort, clock);
    }

    @Bean
    public AvisoService avisoService(AvisoRepository avisoRepository) {
        return new AvisoService(avisoRepository);
    }

    @Bean
    public ReglaStockBajo reglaStockBajo() {
        return new ReglaStockBajo();
    }
}
