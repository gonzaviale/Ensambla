package com.ensambla.backend.infrastructure.presentation.dto.zona;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Un lugar escrito (barrio o dirección) o un punto del mapa (latitud y longitud)")
public record AnalizarZonaWebRequest(
        @Size(min = 2, max = 120) String lugar,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitud,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitud) {

    @Schema(hidden = true)
    @AssertTrue(message = "Indicá un lugar o un punto del mapa")
    public boolean isLugarOPuntoIndicado() {
        boolean hayLugar = lugar != null && !lugar.isBlank();
        boolean hayPunto = latitud != null && longitud != null;
        return hayLugar || hayPunto;
    }
}
