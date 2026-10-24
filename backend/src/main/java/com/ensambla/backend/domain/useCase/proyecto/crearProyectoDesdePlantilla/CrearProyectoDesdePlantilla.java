package com.ensambla.backend.domain.useCase.proyecto.crearProyectoDesdePlantilla;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CrearProyectoDesdePlantilla {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    private final PlantillaService plantillaService;

    private final Clock clock;

    public ProyectoItem execute(CrearProyectoDesdePlantillaRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        PlantillaProyecto plantilla = plantillaService.obtenerParaProyecto(request.plantillaId());
        Proyecto proyecto = proyectoService.crearDesdePlantilla(negocioId, plantilla, OffsetDateTime.now(clock));
        return ProyectoItem.de(proyecto);
    }
}

