package com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo;

import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivoId;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ResponderMiniObjetivoResponse(
        RespuestaMiniObjetivoId id,
        MiniObjetivoId miniObjetivoId,
        HizoManual hizo,
        QuePaso quePaso,
        String comentario,
        LocalDate semanaInicio,
        OffsetDateTime respondidaEn) {

    public static ResponderMiniObjetivoResponse de(RespuestaMiniObjetivo respuesta) {
        return new ResponderMiniObjetivoResponse(
                respuesta.id(),
                respuesta.miniObjetivoId(),
                respuesta.hizo(),
                respuesta.quePaso(),
                respuesta.comentario(),
                respuesta.semanaInicio(),
                respuesta.respondidaEn());
    }
}
