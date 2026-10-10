package com.ensambla.backend.infrastructure.presentation.dto.registro;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record RegistroRequest(
        @NotNull(message = "Los valores son obligatorios")
        Map<String, Object> valores
) {
}
