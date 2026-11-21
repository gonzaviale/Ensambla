package com.ensambla.backend.infrastructure.presentation.dto.metrica;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** El lugar nuevo de la métrica en el panel; el primero es el 0. */
public record MoverMetricaWebRequest(@NotNull @Min(0) Integer lugar) {
}
