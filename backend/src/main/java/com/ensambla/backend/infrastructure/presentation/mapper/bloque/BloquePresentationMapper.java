package com.ensambla.backend.infrastructure.presentation.mapper.bloque;

import com.ensambla.backend.domain.dto.bloque.BloqueItem;
import com.ensambla.backend.domain.dto.bloque.CrearBloqueRequest;
import com.ensambla.backend.domain.dto.bloque.EliminarBloqueRequest;
import com.ensambla.backend.domain.dto.bloque.ListarBloquesRequest;
import com.ensambla.backend.domain.dto.bloque.RenombrarBloqueRequest;
import com.ensambla.backend.infrastructure.presentation.dto.bloque.BloqueResponse;
import com.ensambla.backend.infrastructure.presentation.dto.bloque.BloqueWebRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class BloquePresentationMapper {

    public ListarBloquesRequest toListarRequest(UUID usuarioId, UUID proyectoId) {
        return new ListarBloquesRequest(usuarioId, proyectoId);
    }

    public CrearBloqueRequest toCrearRequest(UUID usuarioId, UUID proyectoId, BloqueWebRequest request) {
        return new CrearBloqueRequest(usuarioId, proyectoId, request.nombre());
    }

    public RenombrarBloqueRequest toRenombrarRequest(UUID usuarioId, UUID bloqueId, BloqueWebRequest request) {
        return new RenombrarBloqueRequest(usuarioId, bloqueId, request.nombre());
    }

    public EliminarBloqueRequest toEliminarRequest(UUID usuarioId, UUID bloqueId) {
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
