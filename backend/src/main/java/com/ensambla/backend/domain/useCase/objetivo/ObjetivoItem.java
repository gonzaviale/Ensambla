package com.ensambla.backend.domain.useCase.objetivo;

import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ObjetivoItem(
        ObjetivoId id,
        String texto,
        TipoObjetivo tipo,
        BigDecimal meta,
        boolean armadoConSami,
        OffsetDateTime creadoEn) {

    public static ObjetivoItem de(Objetivo objetivo) {
        return new ObjetivoItem(
                objetivo.getId(),
                objetivo.getTexto(),
                objetivo.getTipo(),
                objetivo.getMeta(),
                objetivo.isArmadoConSami(),
                objetivo.getCreadoEn());
    }
}
