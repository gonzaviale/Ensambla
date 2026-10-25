package com.ensambla.backend.domain.useCase.bloque.listarBloques;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarBloques {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    public List<BloqueItem> execute(ListarBloquesRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoRepository.obtener(negocioId, request.proyectoId());
        return proyecto.getBloques().stream().map(BloqueItem::de).toList();
    }
}
