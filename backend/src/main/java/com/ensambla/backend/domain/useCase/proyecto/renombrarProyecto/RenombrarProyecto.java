package com.ensambla.backend.domain.useCase.proyecto.renombrarProyecto;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class RenombrarProyecto {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    private final ProyectoRepository proyectoRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public ProyectoItem execute(RenombrarProyectoRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return ProyectoItem.de(transaccionPort.ejecutar(
                () -> renombrar(negocioId, request.proyectoId(), request.nombre(), ahora)));
    }

    private Proyecto renombrar(NegocioId negocioId, ProyectoId proyectoId, String nombre, OffsetDateTime ahora) {
        Proyecto proyecto = proyectoService.obtener(negocioId, proyectoId);
        proyecto.renombrar(nombre, ahora);
        return proyectoRepository.save(proyecto);
    }
}

