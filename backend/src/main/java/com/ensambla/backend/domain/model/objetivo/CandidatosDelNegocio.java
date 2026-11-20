package com.ensambla.backend.domain.model.objetivo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que el negocio ofrece hoy para sus objetivos: el resultado de los últimos 30 días y los mini-objetivos posibles,
 * del que más suma al que menos.
 */
public record CandidatosDelNegocio(BigDecimal resultado, List<CandidatoMiniObjetivo> candidatos) {

    /** Si el resultado de hoy ya llega a la meta: no hay nada que proponer. */
    public boolean alcanza(BigDecimal meta) {
        return resultado.compareTo(meta) >= 0;
    }
}
