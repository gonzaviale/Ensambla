package com.ensambla.backend.infrastructure.presentation.dto.proyecto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearProyectoWebRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 2000) String descripcion
) {
}

