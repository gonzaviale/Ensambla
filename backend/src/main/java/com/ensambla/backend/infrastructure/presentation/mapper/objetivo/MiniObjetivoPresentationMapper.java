package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.MiniObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.abandonarMiniObjetivo.AbandonarMiniObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosResponse.CandidatoItem;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivosRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivosResponse;
import com.ensambla.backend.domain.useCase.objetivo.reemplazarMiniObjetivo.ReemplazarMiniObjetivoRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.CandidatoMiniObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.MiniObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.MiniObjetivosResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ReemplazarMiniObjetivoWebRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MiniObjetivoPresentationMapper {

    public ListarMiniObjetivosRequest toListarRequest(UsuarioId usuarioId, ObjetivoId objetivoId) {
        return new ListarMiniObjetivosRequest(usuarioId, objetivoId);
    }

    public ListarCandidatosRequest toCandidatosRequest(
            UsuarioId usuarioId, ObjetivoId objetivoId, MiniObjetivoId miniObjetivoId) {
        return new ListarCandidatosRequest(usuarioId, objetivoId, miniObjetivoId);
    }

    public ReemplazarMiniObjetivoRequest toReemplazarRequest(
            UsuarioId usuarioId, ObjetivoId objetivoId, MiniObjetivoId miniObjetivoId,
            ReemplazarMiniObjetivoWebRequest request) {
        return new ReemplazarMiniObjetivoRequest(
                usuarioId, objetivoId, miniObjetivoId, request.tipo(), request.registroId(), request.referencia());
    }

    public AbandonarMiniObjetivoRequest toAbandonarRequest(
            UsuarioId usuarioId, ObjetivoId objetivoId, MiniObjetivoId miniObjetivoId) {
        return new AbandonarMiniObjetivoRequest(usuarioId, objetivoId, miniObjetivoId);
    }

    public MiniObjetivosResponse toResponse(ListarMiniObjetivosResponse response) {
        return new MiniObjetivosResponse(
                response.minis().stream().map(this::toResponse).toList(),
                response.historial().stream().map(this::toResponse).toList(),
                response.faltanDatos());
    }

    public MiniObjetivoResponse toResponse(MiniObjetivoItem item) {
        return new MiniObjetivoResponse(
                item.id(),
                item.posicion(),
                item.tipo(),
                item.sentido(),
                item.estado(),
                item.titulo(),
                item.justificacion(),
                item.referencia(),
                item.registroId(),
                item.valorInicial(),
                item.valorActual(),
                item.meta(),
                item.avance(),
                item.aporteEstimado(),
                item.semanaInicio(),
                item.textoConIa());
    }

    public List<CandidatoMiniObjetivoResponse> toResponse(ListarCandidatosResponse response) {
        return response.content().stream().map(this::toResponse).toList();
    }

    private CandidatoMiniObjetivoResponse toResponse(CandidatoItem item) {
        return new CandidatoMiniObjetivoResponse(
                item.tipo(),
                item.sentido(),
                item.titulo(),
                item.justificacion(),
                item.referencia(),
                item.registroId(),
                item.valorInicial(),
                item.meta(),
                item.aporteEstimado());
    }
}
