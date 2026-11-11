package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CrearObjetivoWebRequest(
        @NotBlank @Size(max = 500) String texto,
        @NotNull @DecimalMin("1") @Digits(integer = 12, fraction = 2) BigDecimal meta
) {
}
