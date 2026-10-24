package com.ensambla.backend.domain.useCase.sami.conversarConSami;

import com.ensambla.backend.domain.model.sami.RespuestaSami;

import java.util.List;
import java.util.UUID;

public record ConversarConSamiRequest(UUID usuarioId, String texto, List<RespuestaSamiItem> respuestas) {

    public List<RespuestaSami> respuestasSami() {
        return respuestas.stream().map(RespuestaSamiItem::aRespuestaSami).toList();
    }
}
