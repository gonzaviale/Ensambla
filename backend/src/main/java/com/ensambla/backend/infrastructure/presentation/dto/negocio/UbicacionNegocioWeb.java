package com.ensambla.backend.infrastructure.presentation.dto.negocio;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UbicacionNegocioWeb(
        @NotBlank @Size(max = 500) String direccion,
        @NotBlank @Size(max = 120) String localidad,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitud,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitud) {
}
