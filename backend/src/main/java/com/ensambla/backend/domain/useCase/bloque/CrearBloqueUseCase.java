package com.ensambla.backend.domain.useCase.bloque;

import com.ensambla.backend.domain.dto.bloque.BloqueItem;
import com.ensambla.backend.domain.dto.bloque.CrearBloqueRequest;
import com.ensambla.backend.domain.mapper.bloque.CrearBloqueMapper;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearBloqueUseCase {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    private final BloqueService bloqueService;

    private final CrearBloqueMapper crearBloqueMapper;

    private final Clock clock;

    public BloqueItem execute(CrearBloqueRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoService.obtener(negocioId, request.proyectoId());
        Bloque bloque = bloqueService.crear(proyecto, request.nombre(), OffsetDateTime.now(clock));
        return crearBloqueMapper.toResponse(bloque);
    }
}
