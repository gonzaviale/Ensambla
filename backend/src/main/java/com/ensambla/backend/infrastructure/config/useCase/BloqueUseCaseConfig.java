package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
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
            NegocioService negocioService,
            ProyectoService proyectoService) {
        return new ListarBloques(
                negocioService,
                proyectoService);
    }

    @Bean
    public CrearBloque crearBloque(
            NegocioService negocioService,
            ProyectoService proyectoService,
            BloqueService bloqueService,
            Clock clock) {
        return new CrearBloque(
                negocioService,
                proyectoService,
                bloqueService,
                clock);
    }

    @Bean
    public RenombrarBloque renombrarBloque(
            NegocioService negocioService,
            BloqueService bloqueService,
            ProyectoService proyectoService,
            BloqueRepository bloqueRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new RenombrarBloque(
                negocioService,
                bloqueService,
                proyectoService,
                bloqueRepository,
                transaccionPort,
                clock);
    }

    @Bean
    public EliminarBloque eliminarBloque(
            NegocioService negocioService,
            BloqueService bloqueService,
            BloqueRepository bloqueRepository,
            CampoRepository campoRepository,
            TransaccionPort transaccionPort) {
        return new EliminarBloque(
                negocioService,
                bloqueService,
                bloqueRepository,
                campoRepository,
                transaccionPort);
    }

    @Bean
    public BloqueService bloqueService(
            BloqueRepository bloqueRepository,
            TransaccionPort transaccionPort) {
        return new BloqueService(bloqueRepository, transaccionPort);
    }
}
