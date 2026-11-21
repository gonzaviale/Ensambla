package com.ensambla.backend.domain.useCase.objetivo.listarDecisiones;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.EfectoDecision;
import com.ensambla.backend.domain.useCase.objetivo.DecisionItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Las decisiones de la persona frente a un objetivo, agrupadas por la semana en que las tomó.
 *
 * @param semanas de la semana más nueva a la más vieja
 */
public record ListarDecisionesResponse(List<SemanaDecisiones> semanas) {

    /** Las decisiones de la semana que empieza el {@code semanaInicio}, de la más nueva a la más vieja. */
    public record SemanaDecisiones(LocalDate semanaInicio, List<DecisionItem> decisiones) {
    }

    /**
     * @param decisiones las del objetivo, en cualquier orden
     * @param avisos     los avisos de esas decisiones que todavía existen, por id, para leer su efecto
     */
    public static ListarDecisionesResponse de(List<DecisionObjetivo> decisiones, Map<AvisoId, Aviso> avisos) {
        Map<LocalDate, List<DecisionItem>> porSemana = new LinkedHashMap<>();
        decisiones.stream()
                .sorted((una, otra) -> otra.tomadaEn().compareTo(una.tomadaEn()))
                .forEach(decision -> porSemana
                        .computeIfAbsent(decision.semanaInicio(), semana -> new ArrayList<>())
                        .add(DecisionItem.de(decision, EfectoDecision.de(decision, avisoDe(decision, avisos)))));
        return new ListarDecisionesResponse(porSemana.entrySet().stream()
                .sorted((una, otra) -> otra.getKey().compareTo(una.getKey()))
                .map(semana -> new SemanaDecisiones(semana.getKey(), semana.getValue()))
                .toList());
    }

    /** El aviso de la decisión; {@code null} si no es sobre un aviso o si el aviso ya no existe. */
    private static Aviso avisoDe(DecisionObjetivo decision, Map<AvisoId, Aviso> avisos) {
        return decision.avisoId() == null ? null : avisos.get(decision.avisoId());
    }
}
