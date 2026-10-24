package com.ensambla.backend.infrastructure.presentation.mapper.bloque;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
import com.ensambla.backend.domain.useCase.bloque.crearBloque.CrearBloqueRequest;
import com.ensambla.backend.domain.useCase.bloque.eliminarBloque.EliminarBloqueRequest;
import com.ensambla.backend.domain.useCase.bloque.listarBloques.ListarBloquesRequest;
import com.ensambla.backend.domain.useCase.bloque.renombrarBloque.RenombrarBloqueRequest;
import com.ensambla.backend.infrastructure.presentation.dto.bloque.BloqueResponse;
import com.ensambla.backend.infrastructure.presentation.dto.bloque.BloqueWebRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BloquePresentationMapper {

    public ListarBloquesRequest toListarRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
        return new ListarBloquesRequest(usuarioId, proyectoId);
    }

    public CrearBloqueRequest toCrearRequest(UsuarioId usuarioId, ProyectoId proyectoId, BloqueWebRequest request) {
        return new CrearBloqueRequest(usuarioId, proyectoId, request.nombre());
    }

    public RenombrarBloqueRequest toRenombrarRequest(UsuarioId usuarioId, BloqueId bloqueId, BloqueWebRequest request) {
        return new RenombrarBloqueRequest(usuarioId, bloqueId, request.nombre());
    }

    public EliminarBloqueRequest toEliminarRequest(UsuarioId usuarioId, BloqueId bloqueId) {
        return new EliminarBloqueRequest(usuarioId, bloqueId);
    }

    public List<BloqueResponse> toResponse(List<BloqueItem> bloques) {
        return bloques.stream().map(this::toResponse).toList();
    }

    public BloqueResponse toResponse(BloqueItem bloque) {
        return new BloqueResponse(
                bloque.id(),
                bloque.proyectoId(),
                bloque.nombre(),
                bloque.posicion(),
                bloque.creadoEn(),
                bloque.actualizadoEn());
    }
}
