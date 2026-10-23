package com.ensambla.backend.domain.useCase.sami;

import com.ensambla.backend.domain.dto.proyecto.ProyectoItem;
import com.ensambla.backend.domain.dto.sami.CrearProyectoDesdeSamiRequest;
import com.ensambla.backend.domain.mapper.sami.CrearProyectoDesdeSamiMapper;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearProyectoDesdeSamiUseCase {

    private final NegocioService negocioService;

    private final PlantillaService plantillaService;

    private final ProyectoService proyectoService;

    private final CrearProyectoDesdeSamiMapper crearProyectoDesdeSamiMapper;

    private final Clock clock;

    public ProyectoItem execute(CrearProyectoDesdeSamiRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        PlantillaProyecto plantilla = plantillaService.obtenerParaProyecto(request.plantillaId());
        Proyecto proyecto = proyectoService.crearDesdePlantilla(
                negocioId,
                plantilla,
                request.nombre(),
                crearProyectoDesdeSamiMapper.toColumnas(request.columnas()),
                OffsetDateTime.now(clock));
        return crearProyectoDesdeSamiMapper.toResponse(proyecto);
    }
}
