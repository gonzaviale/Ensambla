package com.ensambla.backend.domain.useCase.bloque.listarBloques;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarBloques {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    public List<BloqueItem> execute(ListarBloquesRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoService.obtener(negocioId, request.proyectoId());
        return proyecto.getBloques().stream().map(BloqueItem::de).toList();
    }
}
