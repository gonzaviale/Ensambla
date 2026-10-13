package com.ensambla.backend.infrastructure.presentation.mapper.campo;

import com.ensambla.backend.domain.dto.campo.CampoItem;
import com.ensambla.backend.domain.dto.campo.CrearCampoRequest;
import com.ensambla.backend.domain.dto.campo.EditarCampoRequest;
import com.ensambla.backend.domain.dto.campo.EliminarCampoRequest;
import com.ensambla.backend.domain.dto.campo.ListarCamposRequest;
import com.ensambla.backend.infrastructure.presentation.dto.campo.CampoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.campo.CampoWebRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CampoPresentationMapper {

    public ListarCamposRequest toListarRequest(UUID usuarioId, UUID bloqueId) {
        return new ListarCamposRequest(usuarioId, bloqueId);
    }

    public CrearCampoRequest toCrearRequest(UUID usuarioId, UUID bloqueId, CampoWebRequest request) {
        return new CrearCampoRequest(
                usuarioId,
                bloqueId,
                request.nombre(),
                request.tipo(),
                request.obligatorio(),
                request.valorInicial(),
                request.opciones(),
                request.bloqueRelacionadoId());
    }

    public EditarCampoRequest toEditarRequest(UUID usuarioId, UUID campoId, CampoWebRequest request) {
        return new EditarCampoRequest(
                usuarioId,
                campoId,
                request.nombre(),
                request.tipo(),
                request.obligatorio(),
                request.valorInicial(),
                request.opciones(),
                request.bloqueRelacionadoId());
    }

    public EliminarCampoRequest toEliminarRequest(UUID usuarioId, UUID campoId) {
        return new EliminarCampoRequest(usuarioId, campoId);
    }

    public List<CampoResponse> toResponse(List<CampoItem> campos) {
        return campos.stream().map(this::toResponse).toList();
    }

    public CampoResponse toResponse(CampoItem campo) {
        return new CampoResponse(
                campo.id(),
                campo.bloqueId(),
                campo.nombre(),
                campo.tipo(),
                campo.obligatorio(),
                campo.valorInicial(),
                campo.opciones(),
                campo.bloqueRelacionadoId(),
                campo.posicion(),
                campo.creadoEn(),
                campo.actualizadoEn());
    }
}
