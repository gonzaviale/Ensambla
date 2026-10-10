package com.ensambla.backend.domain.useCase.bloque;

import com.ensambla.backend.domain.dto.bloque.BloqueItem;
import com.ensambla.backend.domain.dto.bloque.RenombrarBloqueRequest;
import com.ensambla.backend.domain.mapper.bloque.RenombrarBloqueMapper;
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
public class RenombrarBloqueUseCase {

    private final NegocioService negocioService;

    private final BloqueService bloqueService;

    private final ProyectoService proyectoService;

    private final RenombrarBloqueMapper renombrarBloqueMapper;

    private final Clock clock;

    public BloqueItem execute(RenombrarBloqueRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        Proyecto proyecto = proyectoService.obtener(negocioId, bloque.getProyectoId());
        Bloque renombrado = bloqueService.renombrar(proyecto, bloque, request.nombre(), OffsetDateTime.now(clock));
        return renombrarBloqueMapper.toResponse(renombrado);
    }
}
