package com.ensambla.backend.infrastructure.presentation.mapper.sistema;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.CampoItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.OpcionesItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem.SeccionItem;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
import com.ensambla.backend.domain.useCase.sistema.eliminarSistema.EliminarSistemaRequest;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistemaRequest;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemasRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sistema.SistemaWebRequest.BloqueWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sistema.SistemaWebRequest.CampoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sistema.SistemaWebRequest.SeccionWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sistema.SistemaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sistema.SistemaWebResponse.BloqueWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.sistema.SistemaWebResponse.CampoWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.sistema.SistemaWebResponse.OpcionesWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.sistema.SistemaWebResponse.SeccionWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.sistema.SistemaWebResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SistemaPresentationMapper {

    public ListarSistemasRequest toListarRequest(UsuarioId usuarioId) {
        return new ListarSistemasRequest(usuarioId);
    }

    public GuardarSistemaRequest toGuardarRequest(UsuarioId usuarioId, String id, SistemaWebRequest request) {
        return new GuardarSistemaRequest(usuarioId, id, toItem(request));
    }

    public EliminarSistemaRequest toEliminarRequest(UsuarioId usuarioId, String id) {
        return new EliminarSistemaRequest(usuarioId, id);
    }

    public List<SistemaWebResponse> toResponse(List<SistemaItem> sistemas) {
        return sistemas.stream().map(this::toResponse).toList();
    }

    public SistemaWebResponse toResponse(SistemaItem sistema) {
        return new SistemaWebResponse(
                sistema.id(),
                sistema.name(),
                sistema.createdAt(),
                sistema.updatedAt(),
                sistema.sections().stream().map(this::toResponse).toList(),
                sistema.template(),
                new OpcionesWebResponse(sistema.options().movements(), sistema.options().expenses()),
                sistema.layout(),
                sistema.demoAlmacen());
    }

    private SeccionWebResponse toResponse(SeccionItem seccion) {
        return new SeccionWebResponse(
                seccion.id(), seccion.name(), seccion.blocks().stream().map(this::toResponse).toList());
    }

    private BloqueWebResponse toResponse(BloqueItem bloque) {
        return new BloqueWebResponse(
                bloque.id(),
                bloque.type(),
                bloque.name(),
                bloque.description(),
                bloque.color(),
                bloque.width(),
                bloque.fields().stream().map(this::toResponse).toList(),
                bloque.rows(),
                bloque.content(),
                bloque.startRow(),
                bloque.stockDirection());
    }

    private CampoWebResponse toResponse(CampoItem campo) {
        return new CampoWebResponse(
                campo.key(), campo.label(), campo.type(), campo.required(), campo.options(), campo.min(),
                campo.target());
    }

    private SistemaItem toItem(SistemaWebRequest request) {
        return new SistemaItem(
                request.id(),
                request.name(),
                request.createdAt(),
                request.updatedAt(),
                request.sections().stream().map(this::toItem).toList(),
                request.template(),
                request.options() == null
                        ? null
                        : new OpcionesItem(request.options().movements(), request.options().expenses()),
                request.layout(),
                Boolean.TRUE.equals(request.demoAlmacen()));
    }

    private SeccionItem toItem(SeccionWebRequest seccion) {
        return new SeccionItem(seccion.id(), seccion.name(), seccion.blocks().stream().map(this::toItem).toList());
    }

    private BloqueItem toItem(BloqueWebRequest bloque) {
        return new BloqueItem(
                bloque.id(),
                bloque.type(),
                bloque.name(),
                bloque.description(),
                bloque.color(),
                bloque.width(),
                bloque.fields().stream().map(this::toItem).toList(),
                bloque.rows(),
                bloque.content(),
                bloque.startRow(),
                bloque.stockDirection());
    }

    private CampoItem toItem(CampoWebRequest campo) {
        return new CampoItem(
                campo.key(), campo.label(), campo.type(), campo.required(), campo.options(), campo.min(),
                campo.target());
    }
}
