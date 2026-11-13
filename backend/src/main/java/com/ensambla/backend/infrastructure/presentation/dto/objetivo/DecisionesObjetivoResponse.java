package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import java.time.LocalDate;
import java.util.List;

/** Las decisiones de la persona frente a un objetivo, por semana, de la más nueva a la más vieja. */
public record DecisionesObjetivoResponse(List<SemanaDecisionesResponse> semanas) {

    /** Las decisiones de la semana que empieza el {@code semanaInicio}, de la más nueva a la más vieja. */
    public record SemanaDecisionesResponse(LocalDate semanaInicio, List<DecisionObjetivoResponse> decisiones) {
    }
}
