package com.ensambla.backend.domain.useCase.bloque.crearBloque;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = obtenerProyecto(negocioId, request.proyectoId());
        Bloque bloque = guardar(proyecto.agregarBloque(request.nombre(), OffsetDateTime.now(clock)));
        return BloqueItem.de(bloque);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Proyecto obtenerProyecto(NegocioId negocioId, ProyectoId proyectoId) {
        return proyectoRepository.obtener(negocioId, proyectoId);
    }

    private Bloque guardar(Bloque bloque) {
        return bloqueRepository.save(bloque);
    }
}
