package com.ensambla.backend.domain.useCase.bloque.crearBloque;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CrearBloque {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    private final BloqueService bloqueService;

    private final Clock clock;

    public BloqueItem execute(CrearBloqueRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoService.obtener(negocioId, request.proyectoId());
        Bloque bloque = bloqueService.crear(proyecto, request.nombre(), OffsetDateTime.now(clock));
        return BloqueItem.de(bloque);
    }
}
