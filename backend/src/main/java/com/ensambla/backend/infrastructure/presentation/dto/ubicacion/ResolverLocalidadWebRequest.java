package com.ensambla.backend.infrastructure.presentation.dto.ubicacion;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ResolverLocalidadWebRequest(
        @NotNull(message = "Falta la latitud")
        @DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90") BigDecimal latitud,
        @NotNull(message = "Falta la longitud")
        @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180") BigDecimal longitud) {
}
