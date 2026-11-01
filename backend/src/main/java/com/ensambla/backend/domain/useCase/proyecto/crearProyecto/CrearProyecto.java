package com.ensambla.backend.domain.useCase.proyecto.crearProyecto;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CrearProyecto {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final ControlPlanService controlPlanService;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public ProyectoItem execute(CrearProyectoRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        Proyecto proyecto = transaccionPort.ejecutar(() -> crear(negocioId, request, ahora));
        return ProyectoItem.de(proyecto);
    }

    private Proyecto crear(NegocioId negocioId, CrearProyectoRequest request, OffsetDateTime ahora) {
        controlPlanService.validarAltaProyecto(negocioId, ahora);
        return proyectoRepository.save(Proyecto.enBlanco(negocioId, request.nombre(), request.descripcion(), ahora));
    }
}
