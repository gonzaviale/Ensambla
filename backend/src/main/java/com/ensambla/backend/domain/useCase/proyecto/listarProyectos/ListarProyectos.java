package com.ensambla.backend.domain.useCase.proyecto.listarProyectos;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarProyectos {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    public List<ProyectoItem> execute(ListarProyectosRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        List<Proyecto> proyectos = obtenerProyectos(negocioId);
        return proyectos.stream().map(ProyectoItem::de).toList();
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private List<Proyecto> obtenerProyectos(NegocioId negocioId) {
        return proyectoRepository.findAllByNegocioId(negocioId);
    }
}
