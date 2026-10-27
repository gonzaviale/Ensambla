package com.ensambla.backend.domain.useCase.sami.conversarConSami;

import com.ensambla.backend.domain.model.sami.RespuestaSami;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;

public record ConversarConSamiRequest(UsuarioId usuarioId, String texto, List<RespuestaSamiItem> respuestas) {

    public List<RespuestaSami> respuestasSami() {
        return respuestas.stream().map(RespuestaSamiItem::aRespuestaSami).toList();
    }
}
