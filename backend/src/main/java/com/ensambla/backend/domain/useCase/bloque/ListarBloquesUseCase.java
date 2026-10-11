package com.ensambla.backend.domain.useCase.bloque;

import com.ensambla.backend.domain.dto.bloque.BloqueItem;
import com.ensambla.backend.domain.dto.bloque.ListarBloquesRequest;
import com.ensambla.backend.domain.mapper.bloque.ListarBloquesMapper;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListarBloquesUseCase {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    private final ListarBloquesMapper listarBloquesMapper;

    public List<BloqueItem> execute(ListarBloquesRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoService.obtener(negocioId, request.proyectoId());
        return listarBloquesMapper.toResponse(proyecto.getBloques());
    }
}
