package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
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
    public ObtenerFotoLote obtenerFotoLote(NegocioRepository negocioRepository, ArchivoCuadernoPort archivoCuadernoPort,
            LoteSubidaRepository loteSubidaRepository) {
        return new ObtenerFotoLote(negocioRepository, archivoCuadernoPort, loteSubidaRepository);
    }

    @Bean
    public SubirLote subirLote(
            NegocioRepository negocioRepository,
            LoteSubidaRepository loteSubidaRepository,
            ArchivoCuadernoPort archivoCuadernoPort,
            ControlPlanService controlPlanService,
            TransaccionPort transaccionPort,
            BloqueRepository bloqueRepository,
            Clock clock) {
        return new SubirLote(
                negocioRepository, loteSubidaRepository, archivoCuadernoPort, controlPlanService, transaccionPort,
                bloqueRepository, clock);
    }

    @Bean
    public ObtenerLote obtenerLote(
            NegocioRepository negocioRepository,
            LoteSubidaRepository loteSubidaRepository) {
        return new ObtenerLote(negocioRepository,
                loteSubidaRepository);
    }

}
