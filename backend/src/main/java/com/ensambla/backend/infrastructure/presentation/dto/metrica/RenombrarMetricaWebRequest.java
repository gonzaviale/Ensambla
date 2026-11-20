package com.ensambla.backend.infrastructure.presentation.dto.metrica;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El nombre nuevo que se ve en el panel. */
public record RenombrarMetricaWebRequest(@NotBlank @Size(max = 80) String nombre) {
}
