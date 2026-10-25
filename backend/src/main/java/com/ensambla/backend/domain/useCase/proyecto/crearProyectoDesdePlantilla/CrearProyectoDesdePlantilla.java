package com.ensambla.backend.domain.useCase.proyecto.crearProyectoDesdePlantilla;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CrearProyectoDesdePlantilla {

    private final NegocioRepository negocioRepository;

    private final PlantillaRepository plantillaRepository;

    private final ProyectoRepository proyectoRepository;

    private final ControlPlanService controlPlanService;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public ProyectoItem execute(CrearProyectoDesdePlantillaRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        PlantillaProyecto plantilla = plantillaRepository.obtenerParaProyecto(request.plantillaId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        Proyecto proyecto = transaccionPort.ejecutar(() -> crear(negocioId, plantilla, ahora));
        return ProyectoItem.de(proyecto);
    }

    /** Guarda toda la estructura de la plantilla en la misma operación que el control del cupo. */
    private Proyecto crear(NegocioId negocioId, PlantillaProyecto plantilla, OffsetDateTime ahora) {
        controlPlanService.validarAltaProyecto(negocioId, ahora);
        return proyectoRepository.saveConEstructura(Proyecto.desdePlantilla(negocioId, plantilla, ahora));
    }
}
