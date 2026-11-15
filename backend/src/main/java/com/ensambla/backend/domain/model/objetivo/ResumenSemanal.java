package com.ensambla.backend.domain.model.objetivo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * El cierre del domingo de un objetivo: cómo terminó la semana, qué decidió la persona y qué se reajustó. Hay uno solo
 * por objetivo y semana.
 *
 * @param semanaInicio el lunes de la semana cerrada
 * @param resultado    lo que quedaba de los últimos 30 días al domingo
 * @param avance       qué parte de la meta es ese resultado, de 0 a 1
 * @param minis        el valor de cada mini-objetivo que se pudo medir al cierre
 * @param queFunciono  el resumen en palabras de la persona
 */
public record ResumenSemanal(
        ResumenSemanalId id,
        ObjetivoId objetivoId,
        LocalDate semanaInicio,
        BigDecimal resultado,
        BigDecimal avance,
        ConteoDecisiones decisiones,
        List<CierreMini> minis,
        String queFunciono,
        boolean textoConIa,
        OffsetDateTime cerradoEn,
        List<ReajusteMiniObjetivo> reajustes) {

    public static ResumenSemanal de(
            Objetivo objetivo, LocalDate semanaInicio, BigDecimal resultado, ConteoDecisiones decisiones,
            List<CierreMini> minis, String queFunciono, List<ReajusteMiniObjetivo> reajustes, OffsetDateTime ahora) {
        return new ResumenSemanal(
                ResumenSemanalId.nuevo(),
                objetivo.getId(),
                semanaInicio,
                resultado,
                avanceDe(resultado, objetivo.getMeta()),
                decisiones,
                List.copyOf(minis),
                queFunciono,
                false,
                ahora,
                List.copyOf(reajustes));
    }

    /** El valor que tenía el mini-objetivo en este cierre; vacío si no se pudo medir o no estaba. */
    public Optional<BigDecimal> valorDe(MiniObjetivoId miniObjetivoId) {
        return minis.stream()
                .filter(cierre -> cierre.miniObjetivoId().equals(miniObjetivoId))
                .map(CierreMini::valor)
                .findFirst();
    }

    /** Qué parte de la meta es el resultado, entre 0 y 1; una meta de cero o menos ya está cumplida. */
    static BigDecimal avanceDe(BigDecimal resultado, BigDecimal meta) {
        if (meta.signum() <= 0) {
            return BigDecimal.ONE;
        }
        return resultado.divide(meta, 4, RoundingMode.HALF_UP).max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }
}
