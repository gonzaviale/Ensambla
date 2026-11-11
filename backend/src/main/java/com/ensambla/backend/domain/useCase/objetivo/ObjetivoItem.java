package com.ensambla.backend.domain.useCase.objetivo;

import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ObjetivoItem(ObjetivoId id, String texto, BigDecimal meta, OffsetDateTime creadoEn) {

    public static ObjetivoItem de(Objetivo objetivo) {
        return new ObjetivoItem(
                objetivo.getId(),
                objetivo.getTexto(),
                objetivo.getMeta(),
                objetivo.getCreadoEn());
    }
}
