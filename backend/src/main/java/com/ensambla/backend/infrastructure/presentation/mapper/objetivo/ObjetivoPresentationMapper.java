package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.crearObjetivo.CrearObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.eliminarObjetivo.EliminarObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarObjetivos.ListarObjetivosRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.CrearObjetivoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ObjetivoResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ObjetivoPresentationMapper {

    public ListarObjetivosRequest toListarRequest(UsuarioId usuarioId) {
        return new ListarObjetivosRequest(usuarioId);
    }

    public CrearObjetivoRequest toCrearRequest(UsuarioId usuarioId, CrearObjetivoWebRequest request) {
        return new CrearObjetivoRequest(usuarioId, request.texto(), request.meta());
    }

    public EliminarObjetivoRequest toEliminarRequest(UsuarioId usuarioId, ObjetivoId objetivoId) {
        return new EliminarObjetivoRequest(usuarioId, objetivoId);
    }

    public List<ObjetivoResponse> toResponse(List<ObjetivoItem> objetivos) {
        return objetivos.stream().map(this::toResponse).toList();
    }

    public ObjetivoResponse toResponse(ObjetivoItem objetivo) {
        return new ObjetivoResponse(objetivo.id(), objetivo.texto(), objetivo.meta(), objetivo.creadoEn());
    }
}
