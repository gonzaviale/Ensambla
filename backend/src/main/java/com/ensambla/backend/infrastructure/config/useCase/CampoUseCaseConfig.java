package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.campo.crearCampo.CrearCampo;
import com.ensambla.backend.domain.useCase.campo.editarCampo.EditarCampo;
import com.ensambla.backend.domain.useCase.campo.eliminarCampo.EliminarCampo;
import com.ensambla.backend.domain.useCase.campo.listarCampos.ListarCampos;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CampoUseCaseConfig {

    @Bean
    public ListarCampos listarCampos(
            NegocioService negocioService,
            BloqueService bloqueService,
            CampoService campoService) {
        return new ListarCampos(
                negocioService,
                bloqueService,
                campoService);
    }

    @Bean
    public CrearCampo crearCampo(
            NegocioService negocioService,
            BloqueService bloqueService,
            CampoService campoService,
            Clock clock) {
        return new CrearCampo(
                negocioService,
                bloqueService,
                campoService,
                clock);
    }

    @Bean
    public EditarCampo editarCampo(
            NegocioService negocioService,
            CampoService campoService,
            BloqueService bloqueService,
            CampoRepository campoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new EditarCampo(
                negocioService,
                campoService,
                bloqueService,
                campoRepository,
                transaccionPort,
                clock);
    }

    @Bean
    public EliminarCampo eliminarCampo(
            NegocioService negocioService,
            CampoService campoService,
            CampoRepository campoRepository,
            TransaccionPort transaccionPort) {
        return new EliminarCampo(
                negocioService,
                campoService,
                campoRepository,
                transaccionPort);
    }

    @Bean
    public CampoService campoService(
            CampoRepository campoRepository,
            BloqueRepository bloqueRepository,
            TransaccionPort transaccionPort) {
        return new CampoService(campoRepository, bloqueRepository, transaccionPort);
    }
}
