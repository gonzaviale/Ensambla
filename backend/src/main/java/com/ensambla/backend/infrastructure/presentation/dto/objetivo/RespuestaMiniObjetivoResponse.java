package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivoId;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RespuestaMiniObjetivoResponse(
        RespuestaMiniObjetivoId id,
        MiniObjetivoId miniObjetivoId,
        HizoManual hizo,
        QuePaso quePaso,
        String comentario,
        LocalDate semanaInicio,
        OffsetDateTime respondidaEn) {
}
