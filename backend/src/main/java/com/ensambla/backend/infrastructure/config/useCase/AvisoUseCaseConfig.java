package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.aviso.ReglaAviso;
import com.ensambla.backend.domain.model.aviso.ReglaStockBajo;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
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
            NegocioRepository negocioRepository,
            AvisoRepository avisoRepository,
            ControlPlanService controlPlanService, Clock clock) {
        return new ListarAvisos(negocioRepository, avisoRepository, controlPlanService, clock);
    }

    @Bean
    public ContarAvisos contarAvisos(NegocioRepository negocioRepository, AvisoRepository avisoRepository) {
        return new ContarAvisos(negocioRepository, avisoRepository);
    }

    @Bean
    public AplicarAviso aplicarAviso(
            NegocioRepository negocioRepository,
            ControlPlanService controlPlanService,
            RegistroDatosService registroDatosService,
            RegistroRepository registroRepository,
            CampoRepository campoRepository,
            AvisoRepository avisoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new AplicarAviso(negocioRepository, controlPlanService, registroDatosService, registroRepository,
                campoRepository, avisoRepository, transaccionPort, clock);
    }

    @Bean
    public DescartarAviso descartarAviso(
            NegocioRepository negocioRepository,
            ControlPlanService controlPlanService,
            AvisoRepository avisoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new DescartarAviso(
                negocioRepository, controlPlanService, avisoRepository, transaccionPort, clock);
    }


    @Bean
    public ReglaStockBajo reglaStockBajo() {
        return new ReglaStockBajo();
    }
}
