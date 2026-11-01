package com.ensambla.backend.domain.useCase.sami.conversarConSami;

import com.ensambla.backend.domain.model.sami.RespuestaSami;

public record RespuestaSamiItem(String preguntaId, String opcionId) {

    RespuestaSami aRespuestaSami() {
        return new RespuestaSami(preguntaId, opcionId);
    }
}
