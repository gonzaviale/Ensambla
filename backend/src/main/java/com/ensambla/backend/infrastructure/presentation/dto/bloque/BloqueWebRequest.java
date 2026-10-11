package com.ensambla.backend.infrastructure.presentation.dto.bloque;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body de crear y de renombrar un bloque: en los dos casos sólo viaja el nombre. */
public record BloqueWebRequest(@NotBlank @Size(max = 120) String nombre) {
}
