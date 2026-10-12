package com.ensambla.backend.infrastructure.presentation.mapper.proyecto;

import com.ensambla.backend.domain.dto.proyecto.CrearProyectoDesdePlantillaRequest;
import com.ensambla.backend.domain.dto.proyecto.CrearProyectoRequest;
import com.ensambla.backend.domain.dto.proyecto.EliminarProyectoRequest;
import com.ensambla.backend.domain.dto.proyecto.ListarProyectosRequest;
import com.ensambla.backend.domain.dto.proyecto.ObtenerProyectoRequest;
import com.ensambla.backend.domain.dto.proyecto.ProyectoItem;
import com.ensambla.backend.domain.dto.proyecto.ProyectoItem.BloqueItem;
import com.ensambla.backend.domain.dto.proyecto.ProyectoItem.CampoItem;
import com.ensambla.backend.domain.dto.proyecto.RenombrarProyectoRequest;
import com.ensambla.backend.infrastructure.presentation.dto.proyecto.CrearProyectoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.proyecto.ProyectoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.proyecto.ProyectoResponse.BloqueResponse;
import com.ensambla.backend.infrastructure.presentation.dto.proyecto.ProyectoResponse.CampoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.proyecto.RenombrarProyectoWebRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ProyectoPresentationMapper {

    public ListarProyectosRequest toListarRequest(UUID usuarioId) {
        return new ListarProyectosRequest(usuarioId);
    }

    public ObtenerProyectoRequest toObtenerRequest(UUID usuarioId, UUID proyectoId) {
        return new ObtenerProyectoRequest(usuarioId, proyectoId);
    }

    public CrearProyectoRequest toCrearRequest(UUID usuarioId, CrearProyectoWebRequest request) {
        return new CrearProyectoRequest(usuarioId, request.nombre(), request.descripcion());
    }

    public RenombrarProyectoRequest toRenombrarRequest(
            UUID usuarioId, UUID proyectoId, RenombrarProyectoWebRequest request) {
        return new RenombrarProyectoRequest(usuarioId, proyectoId, request.nombre());
    }

    public CrearProyectoDesdePlantillaRequest toDesdePlantillaRequest(UUID usuarioId, UUID plantillaId) {
        return new CrearProyectoDesdePlantillaRequest(usuarioId, plantillaId);
    }

    public EliminarProyectoRequest toEliminarRequest(UUID usuarioId, UUID proyectoId) {
        return new EliminarProyectoRequest(usuarioId, proyectoId);
    }

    public List<ProyectoResponse> toResponse(List<ProyectoItem> proyectos) {
        return proyectos.stream().map(this::toResponse).toList();
    }

    public ProyectoResponse toResponse(ProyectoItem proyecto) {
        return new ProyectoResponse(
                proyecto.id(),
                proyecto.nombre(),
                proyecto.descripcion(),
                proyecto.plantillaId(),
                proyecto.creadoEn(),
                proyecto.actualizadoEn(),
                proyecto.bloques().stream().map(this::toResponse).toList());
    }

    private BloqueResponse toResponse(BloqueItem bloque) {
        return new BloqueResponse(
                bloque.id(),
                bloque.nombre(),
                bloque.posicion(),
                bloque.campos().stream().map(this::toResponse).toList());
    }

    private CampoResponse toResponse(CampoItem campo) {
        return new CampoResponse(
                campo.id(),
                campo.nombre(),
                campo.tipo(),
                campo.obligatorio(),
                campo.valorInicial(),
                campo.opciones(),
                campo.bloqueRelacionadoId(),
                campo.posicion());
    }
}

