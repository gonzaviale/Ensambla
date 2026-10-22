package com.ensambla.backend.infrastructure.presentation.dto.zona;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalizarZonaWebRequest(@NotBlank @Size(min = 2, max = 120) String lugar) {
}
