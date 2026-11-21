package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.DecisionPaso;
import jakarta.validation.constraints.NotNull;

/** Lo que la persona responde al paso de un mini-objetivo: {@code ACEPTADO} («Lo hago») o {@code IGNORADO}. */
public record DecidirPasoWebRequest(@NotNull(message = "La decisión es obligatoria") DecisionPaso tipo) {
}
