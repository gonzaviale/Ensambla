package com.ensambla.backend.domain.useCase.bloque.crearBloque;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CrearBloque {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final BloqueRepository bloqueRepository;

    private final Clock clock;

    public BloqueItem execute(CrearBloqueRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoRepository.obtener(negocioId, request.proyectoId());
        // El proyecto ya viene validado contra el negocio de la sesión y con sus bloques cargados.
        Bloque bloque = bloqueRepository.save(proyecto.agregarBloque(request.nombre(), OffsetDateTime.now(clock)));
        return BloqueItem.de(bloque);
    }
}
