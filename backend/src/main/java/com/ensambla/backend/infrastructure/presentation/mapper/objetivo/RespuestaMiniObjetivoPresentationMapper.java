package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes.ListarRespuestasPendientesRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes.ListarRespuestasPendientesResponse;
import com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo.ResponderMiniObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo.ResponderMiniObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.RespuestaMiniObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.RespuestaPendienteResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ResponderMiniObjetivoWebRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RespuestaMiniObjetivoPresentationMapper {

    private final MiniObjetivoPresentationMapper miniObjetivoPresentationMapper;

    public ResponderMiniObjetivoRequest toResponderRequest(
            UsuarioId usuarioId, ObjetivoId objetivoId, MiniObjetivoId miniObjetivoId,
            ResponderMiniObjetivoWebRequest request) {
        return new ResponderMiniObjetivoRequest(
                usuarioId, objetivoId, miniObjetivoId, request.hizo(), request.quePaso(), request.comentario());
    }

    public ListarRespuestasPendientesRequest toListarPendientesRequest(UsuarioId usuarioId) {
        return new ListarRespuestasPendientesRequest(usuarioId);
    }

    public RespuestaMiniObjetivoResponse toResponse(ResponderMiniObjetivoResponse response) {
        return new RespuestaMiniObjetivoResponse(
                response.id(),
                response.miniObjetivoId(),
                response.hizo(),
                response.quePaso(),
                response.comentario(),
                response.semanaInicio(),
                response.respondidaEn());
    }

    public List<RespuestaPendienteResponse> toResponse(ListarRespuestasPendientesResponse response) {
        return response.pendientes().stream()
                .map(pendiente -> new RespuestaPendienteResponse(
                        pendiente.objetivoId(),
                        pendiente.objetivoTexto(),
                        miniObjetivoPresentationMapper.toResponse(pendiente.miniObjetivo())))
                .toList();
    }
}
