package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
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
            NegocioRepository negocioRepository,
            BloqueRepository bloqueRepository,
            CampoRepository campoRepository) {
        return new ListarCampos(
                negocioRepository,
                bloqueRepository,
                campoRepository);
    }

    @Bean
    public CrearCampo crearCampo(
            NegocioRepository negocioRepository,
            BloqueRepository bloqueRepository,
            CampoRepository campoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new CrearCampo(
                negocioRepository,
                bloqueRepository,
                campoRepository,
                transaccionPort,
                clock);
    }

    @Bean
    public EditarCampo editarCampo(
            NegocioRepository negocioRepository,
            BloqueRepository bloqueRepository,
            CampoRepository campoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new EditarCampo(
                negocioRepository,
                bloqueRepository,
                campoRepository,
                transaccionPort,
                clock);
    }

    @Bean
    public EliminarCampo eliminarCampo(
            NegocioRepository negocioRepository,
            CampoRepository campoRepository,
            TransaccionPort transaccionPort) {
        return new EliminarCampo(
                negocioRepository,
                campoRepository,
                transaccionPort);
    }

}
