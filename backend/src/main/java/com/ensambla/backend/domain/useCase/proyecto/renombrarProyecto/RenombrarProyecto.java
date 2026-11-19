package com.ensambla.backend.domain.useCase.proyecto.renombrarProyecto;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class RenombrarProyecto {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public ProyectoItem execute(RenombrarProyectoRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return ProyectoItem.de(renombrarEnTransaccion(negocioId, request.proyectoId(), request.nombre(), ahora));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Proyecto renombrarEnTransaccion(
            NegocioId negocioId, ProyectoId proyectoId, String nombre, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> renombrar(negocioId, proyectoId, nombre, ahora));
    }

    private Proyecto renombrar(NegocioId negocioId, ProyectoId proyectoId, String nombre, OffsetDateTime ahora) {
        Proyecto proyecto = obtenerProyecto(negocioId, proyectoId);
        proyecto.renombrar(nombre, ahora);
        return guardar(proyecto);
    }

    private Proyecto obtenerProyecto(NegocioId negocioId, ProyectoId proyectoId) {
        return proyectoRepository.obtener(negocioId, proyectoId);
    }

    private Proyecto guardar(Proyecto proyecto) {
        return proyectoRepository.save(proyecto);
    }
}
