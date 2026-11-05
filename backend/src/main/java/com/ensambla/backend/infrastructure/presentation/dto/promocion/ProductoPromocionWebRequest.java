package com.ensambla.backend.infrastructure.presentation.dto.promocion;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductoPromocionWebRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotNull @Positive @DecimalMax("100000000") BigDecimal precio
) {
}
