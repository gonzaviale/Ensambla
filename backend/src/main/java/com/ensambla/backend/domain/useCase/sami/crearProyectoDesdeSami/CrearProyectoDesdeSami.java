package com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami;

import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearProyectoDesdeSami {

    private final NegocioService negocioService;

    private final PlantillaService plantillaService;

    private final ProyectoService proyectoService;

    private final Clock clock;

    public ProyectoItem execute(CrearProyectoDesdeSamiRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        PlantillaProyecto plantilla = plantillaService.obtenerParaProyecto(request.plantillaId());
        Proyecto proyecto = proyectoService.crearDesdePlantilla(
                negocioId,
                plantilla,
                request.nombre(),
                request.columnasExtra(),
                OffsetDateTime.now(clock));
        return ProyectoItem.de(proyecto);
    }
}
