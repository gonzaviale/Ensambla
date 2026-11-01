package com.ensambla.backend.domain.useCase.proyecto.renombrarProyecto;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
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
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return ProyectoItem.de(transaccionPort.ejecutar(
                () -> renombrar(negocioId, request.proyectoId(), request.nombre(), ahora)));
    }

    private Proyecto renombrar(NegocioId negocioId, ProyectoId proyectoId, String nombre, OffsetDateTime ahora) {
        Proyecto proyecto = proyectoRepository.obtener(negocioId, proyectoId);
        proyecto.renombrar(nombre, ahora);
        return proyectoRepository.save(proyecto);
    }
}

