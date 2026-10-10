package com.ensambla.backend.infrastructure.presentation.dto.proyecto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenombrarProyectoWebRequest(@NotBlank @Size(max = 120) String nombre) {
}

