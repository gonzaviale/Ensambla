package com.ensambla.backend.domain.useCase.objetivo.listarSemanas;

import com.ensambla.backend.domain.model.objetivo.ConteoDecisiones;
import com.ensambla.backend.domain.model.objetivo.ReajusteMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.model.objetivo.TipoReajuste;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Los cierres del domingo de un objetivo, del más nuevo al más viejo.
 *
 * @param ultimoCierre el lunes de la última semana cerrada; vacío si todavía no se cerró ninguna
 */
public record ListarSemanasResponse(LocalDate ultimoCierre, List<SemanaItem> semanas) {

    /**
     * @param semana    el lunes de la semana cerrada
     * @param resultado lo que quedaba de los últimos 30 días al domingo
     * @param avance    qué parte de la meta es ese resultado, de 0 a 1
     */
    public record SemanaItem(
            LocalDate semana,
            BigDecimal resultado,
            BigDecimal avance,
            DecisionesItem decisiones,
            String queFunciono,
            boolean textoConIa,
            List<ReajusteItem> reajustes) {

        public static SemanaItem de(ResumenSemanal resumen) {
            return new SemanaItem(
                    resumen.semanaInicio(),
                    resumen.resultado(),
                    resumen.avance(),
                    DecisionesItem.de(resumen.decisiones()),
                    resumen.queFunciono(),
                    resumen.textoConIa(),
                    resumen.reajustes().stream().map(ReajusteItem::de).toList());
        }
    }

    public record DecisionesItem(int aplicadas, int descartadas, int aceptadas, int ignoradas) {

        public static DecisionesItem de(ConteoDecisiones conteo) {
            return new DecisionesItem(conteo.aplicadas(), conteo.descartadas(), conteo.aceptadas(), conteo.ignoradas());
        }
    }

    /** @param a el mini-objetivo que siguió; vacío si ya no existe */
    public record ReajusteItem(TipoReajuste tipo, String motivo, MiniItem de, MiniItem a) {

        public static ReajusteItem de(ReajusteMiniObjetivo reajuste) {
            return new ReajusteItem(
                    reajuste.tipo(),
                    reajuste.motivo(),
                    new MiniItem(reajuste.tituloAnterior(), reajuste.metaAnterior()),
                    reajuste.tituloNuevo() == null ? null : new MiniItem(reajuste.tituloNuevo(), reajuste.metaNueva()));
        }
    }

    public record MiniItem(String titulo, BigDecimal meta) {
    }

    public static ListarSemanasResponse de(List<ResumenSemanal> resumenes) {
        return new ListarSemanasResponse(
                resumenes.isEmpty() ? null : resumenes.get(0).semanaInicio(),
                resumenes.stream().map(SemanaItem::de).toList());
    }
}
