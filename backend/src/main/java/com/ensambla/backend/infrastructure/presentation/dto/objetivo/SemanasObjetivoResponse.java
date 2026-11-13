package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.TipoReajuste;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Los cierres del domingo de un objetivo, del más nuevo al más viejo.
 *
 * @param ultimoCierre el lunes de la última semana cerrada; vacío si todavía no se cerró ninguna
 */
public record SemanasObjetivoResponse(LocalDate ultimoCierre, List<SemanaResponse> semanas) {

    /** @param avance qué parte de la meta es el resultado, de 0 a 1 */
    public record SemanaResponse(
            LocalDate semana,
            BigDecimal resultado,
            BigDecimal avance,
            DecisionesSemanaResponse decisiones,
            String queFunciono,
            boolean textoConIa,
            List<ReajusteResponse> reajustes) {
    }

    public record DecisionesSemanaResponse(int aplicadas, int descartadas, int aceptadas, int ignoradas) {
    }

    /** @param a el mini-objetivo que siguió; vacío si ya no existe */
    public record ReajusteResponse(TipoReajuste tipo, String motivo, MiniReajusteResponse de, MiniReajusteResponse a) {
    }

    public record MiniReajusteResponse(String titulo, BigDecimal meta) {
    }
}
