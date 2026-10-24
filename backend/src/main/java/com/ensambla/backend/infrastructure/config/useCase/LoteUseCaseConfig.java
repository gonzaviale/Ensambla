package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.lote.obtenerFotoLote.ObtenerFotoLote;
import com.ensambla.backend.domain.useCase.lote.obtenerLote.ObtenerLote;
import com.ensambla.backend.domain.useCase.lote.subirLote.SubirLote;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class LoteUseCaseConfig {

    @Bean
    public ObtenerFotoLote obtenerFotoLote(
            NegocioService negocioService,
            LoteSubidaService loteSubidaService) {
        return new ObtenerFotoLote(
                negocioService,
                loteSubidaService);
    }

    @Bean
    public SubirLote subirLote(
            NegocioService negocioService,
            LoteSubidaService loteSubidaService,
            BloqueService bloqueService,
            Clock clock) {
        return new SubirLote(
                negocioService,
                loteSubidaService,
                bloqueService,
                clock);
    }

    @Bean
    public ObtenerLote obtenerLote(
            NegocioService negocioService,
            LoteSubidaService loteSubidaService) {
        return new ObtenerLote(
                negocioService,
                loteSubidaService);
    }

    @Bean
    public LoteSubidaService loteSubidaService(
            LoteSubidaRepository loteSubidaRepository,
            ArchivoCuadernoPort archivoCuadernoPort,
            ControlPlanService controlPlanService,
            TransaccionPort transaccionPort) {
        return new LoteSubidaService(loteSubidaRepository, archivoCuadernoPort, controlPlanService, transaccionPort);
    }
}
