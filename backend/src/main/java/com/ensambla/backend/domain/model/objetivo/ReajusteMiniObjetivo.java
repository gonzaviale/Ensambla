package com.ensambla.backend.domain.model.objetivo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Un cambio que hizo el cierre semanal: el mini-objetivo que se dejó, el que lo siguió y el porqué, con los títulos y
 * las metas de ese momento para contarlo después («cambiamos X por Y, porque…»).
 */
public record ReajusteMiniObjetivo(
        ReajusteMiniObjetivoId id,
        MiniObjetivoId miniAnteriorId,
        MiniObjetivoId miniNuevoId,
        TipoReajuste tipo,
        String motivo,
        String tituloAnterior,
        BigDecimal metaAnterior,
        String tituloNuevo,
        BigDecimal metaNueva,
        OffsetDateTime creadoEn) {

    public static ReajusteMiniObjetivo de(
            MiniObjetivo anterior, MiniObjetivo nuevo, TipoReajuste tipo, String motivo, OffsetDateTime ahora) {
        return new ReajusteMiniObjetivo(
                ReajusteMiniObjetivoId.nuevo(),
                anterior.getId(),
                nuevo.getId(),
                tipo,
                motivo,
                anterior.getTitulo(),
                anterior.getMeta(),
                nuevo.getTitulo(),
                nuevo.getMeta(),
                ahora);
    }
}
