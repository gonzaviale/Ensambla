package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Algo que la persona decidió frente a un objetivo. Queda una por cada objetivo activo del negocio, para dibujarla en
 * la semana en que la tomó.
 *
 * @param miniObjetivoId el mini-objetivo en curso al que se vincula; vacío si ninguno coincide
 * @param avisoId        el aviso resuelto, si es una decisión sobre un aviso
 * @param registroId     el producto del aviso o del mini-objetivo, si lo hay
 * @param descripcion    el título del aviso o del paso
 * @param semanaInicio   el lunes de la semana (hora argentina) en que se tomó
 */
public record DecisionObjetivo(
        DecisionObjetivoId id,
        NegocioId negocioId,
        ObjetivoId objetivoId,
        MiniObjetivoId miniObjetivoId,
        TipoDecision tipo,
        AvisoId avisoId,
        RegistroId registroId,
        String descripcion,
        OffsetDateTime tomadaEn,
        LocalDate semanaInicio) {

    /** La decisión sobre un aviso, vinculada al mini-objetivo del producto del aviso ({@code null} si no hay). */
    public static DecisionObjetivo deAviso(
            Objetivo objetivo, Aviso aviso, TipoDecision tipo, MiniObjetivo mini, OffsetDateTime ahora) {
        return new DecisionObjetivo(
                DecisionObjetivoId.nuevo(),
                objetivo.getNegocioId(),
                objetivo.getId(),
                mini == null ? null : mini.getId(),
                tipo,
                aviso.getId(),
                aviso.productoId().orElse(null),
                aviso.getTitulo(),
                ahora,
                semanaDe(ahora));
    }

    /** La decisión sobre el paso de un mini-objetivo. */
    public static DecisionObjetivo dePaso(MiniObjetivo mini, DecisionPaso decision, OffsetDateTime ahora) {
        return new DecisionObjetivo(
                DecisionObjetivoId.nuevo(),
                mini.getNegocioId(),
                mini.getObjetivoId(),
                mini.getId(),
                decision.tipo(),
                null,
                mini.getRegistroId(),
                mini.getTitulo(),
                ahora,
                semanaDe(ahora));
    }

    private static LocalDate semanaDe(OffsetDateTime instante) {
        return SemanaObjetivo.lunesDe(ZonaNegocio.dia(instante));
    }
}
