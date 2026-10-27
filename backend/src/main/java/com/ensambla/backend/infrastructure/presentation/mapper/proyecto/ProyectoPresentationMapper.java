package com.ensambla.backend.infrastructure.presentation.mapper.proyecto;

import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem.BloqueItem;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem.CampoItem;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import com.ensambla.backend.domain.useCase.proyecto.crearProyecto.CrearProyectoRequest;
import com.ensambla.backend.domain.useCase.proyecto.crearProyectoDesdePlantilla.CrearProyectoDesdePlantillaRequest;
import com.ensambla.backend.domain.useCase.proyecto.eliminarProyecto.EliminarProyectoRequest;
import com.ensambla.backend.domain.useCase.proyecto.listarProyectos.ListarProyectosRequest;
import com.ensambla.backend.domain.useCase.proyecto.obtenerProyecto.ObtenerProyectoRequest;
import com.ensambla.backend.domain.useCase.proyecto.renombrarProyecto.RenombrarProyectoRequest;
import com.ensambla.backend.infrastructure.presentation.dto.proyecto.CrearProyectoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.proyecto.ProyectoResponse.BloqueResponse;
import com.ensambla.backend.infrastructure.presentation.dto.proyecto.ProyectoResponse.CampoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.proyecto.ProyectoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.proyecto.RenombrarProyectoWebRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProyectoPresentationMapper {

    public ListarProyectosRequest toListarRequest(UsuarioId usuarioId) {
        return new ListarProyectosRequest(usuarioId);
    }

    public ObtenerProyectoRequest toObtenerRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
        return new ObtenerProyectoRequest(usuarioId, proyectoId);
    }

    public CrearProyectoRequest toCrearRequest(UsuarioId usuarioId, CrearProyectoWebRequest request) {
        return new CrearProyectoRequest(usuarioId, request.nombre(), request.descripcion());
    }

    public RenombrarProyectoRequest toRenombrarRequest(
            UsuarioId usuarioId, ProyectoId proyectoId, RenombrarProyectoWebRequest request) {
        return new RenombrarProyectoRequest(usuarioId, proyectoId, request.nombre());
    }

    public CrearProyectoDesdePlantillaRequest toDesdePlantillaRequest(UsuarioId usuarioId, PlantillaId plantillaId) {
        return new CrearProyectoDesdePlantillaRequest(usuarioId, plantillaId);
    }

    public EliminarProyectoRequest toEliminarRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
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
                campo.formula(),
                campo.posicion());
    }
}

