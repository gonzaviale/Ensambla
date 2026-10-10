package com.ensambla.backend.domain.useCase.proyecto;

import com.ensambla.backend.domain.dto.proyecto.CrearProyectoDesdePlantillaRequest;
import com.ensambla.backend.domain.dto.proyecto.ProyectoItem;
import com.ensambla.backend.domain.mapper.proyecto.CrearProyectoDesdePlantillaMapper;
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
public class CrearProyectoDesdePlantillaUseCase {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    private final PlantillaService plantillaService;

    private final CrearProyectoDesdePlantillaMapper crearProyectoDesdePlantillaMapper;

    private final Clock clock;

    public ProyectoItem execute(CrearProyectoDesdePlantillaRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        PlantillaProyecto plantilla = plantillaService.obtenerParaProyecto(request.plantillaId());
        Proyecto proyecto = proyectoService.crearDesdePlantilla(negocioId, plantilla, OffsetDateTime.now(clock));
        return crearProyectoDesdePlantillaMapper.toResponse(proyecto);
    }
}

