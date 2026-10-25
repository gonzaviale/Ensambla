package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.bloque.crearBloque.CrearBloque;
import com.ensambla.backend.domain.useCase.bloque.eliminarBloque.EliminarBloque;
import com.ensambla.backend.domain.useCase.bloque.listarBloques.ListarBloques;
import com.ensambla.backend.domain.useCase.bloque.renombrarBloque.RenombrarBloque;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BloqueUseCaseConfig {

    @Bean
    public ListarBloques listarBloques(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository) {
        return new ListarBloques(
                negocioRepository,
                proyectoRepository);
    }

    @Bean
    public CrearBloque crearBloque(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository,
            BloqueRepository bloqueRepository,
            Clock clock) {
        return new CrearBloque(
                negocioRepository,
                proyectoRepository,
                bloqueRepository,
                clock);
    }

    @Bean
    public RenombrarBloque renombrarBloque(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository,
            BloqueRepository bloqueRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new RenombrarBloque(
                negocioRepository,
                proyectoRepository,
                bloqueRepository,
                transaccionPort,
                clock);
    }

    @Bean
    public EliminarBloque eliminarBloque(
            NegocioRepository negocioRepository,
            BloqueRepository bloqueRepository,
            CampoRepository campoRepository,
            TransaccionPort transaccionPort) {
        return new EliminarBloque(
                negocioRepository,
                bloqueRepository,
                campoRepository,
                transaccionPort);
    }

}
